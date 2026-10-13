package com.dotani.report.service;

import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventParticipation;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.ParticipationStatus;
import com.dotani.event.repository.EventParticipationRepository;
import com.dotani.event.repository.EventRepository;
import com.dotani.member.entity.Gender;
import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.MemberSpecifications;
import com.dotani.member.repository.OrganizationUnitRepository;
import com.dotani.member.service.MemberRoster;
import com.dotani.member.service.MemberRoster.Placement;
import com.dotani.report.dto.ReportDtos.Count;
import com.dotani.report.dto.ReportDtos.EventRow;
import com.dotani.report.dto.ReportDtos.MemberReport;
import com.dotani.report.dto.ReportDtos.OrganizationMembers;
import com.dotani.report.dto.ReportDtos.OrganizationParticipation;
import com.dotani.report.dto.ReportDtos.ParticipationReport;
import com.dotani.report.dto.ReportDtos.ParticipationRow;
import com.dotani.report.dto.ReportDtos.PeriodGranularity;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import com.dotani.shared.time.AppTime;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistics for officers. Ward officers report on the whole ward or one organization; TDP officers only
 * on their own TDP. Everything is computed from the current data (a member who moved counts for the TDP
 * they are in today).
 */
@Service
public class ReportService {

    private static final List<String> AGE_GROUPS = List.of("UNDER_16", "16_20", "21_25", "26_30", "OVER_30", "UNKNOWN");
    private static final Set<EventStatus> REPORTED_EVENT_STATUSES = Set.of(EventStatus.PUBLISHED, EventStatus.COMPLETED);

    private final MemberRepository memberRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final MemberRoster memberRoster;
    private final EventRepository eventRepository;
    private final EventParticipationRepository participationRepository;

    public ReportService(
            MemberRepository memberRepository,
            OrganizationUnitRepository organizationUnitRepository,
            MemberRoster memberRoster,
            EventRepository eventRepository,
            EventParticipationRepository participationRepository
    ) {
        this.memberRepository = memberRepository;
        this.organizationUnitRepository = organizationUnitRepository;
        this.memberRoster = memberRoster;
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
    }

    @Transactional(readOnly = true)
    public MemberReport members(String organizationId, CurrentUser currentUser) {
        OrganizationUnit target = target(organizationId, currentUser);
        List<Member> members = memberRepository.findAll(MemberSpecifications.withinScope(currentUser)).stream()
                .filter(member -> target == null || in(member.getOrganization(), target.getId()))
                .toList();
        List<Member> active = members.stream().filter(member -> member.getMemberStatus() == MemberStatus.ACTIVE).toList();

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (MemberStatus status : MemberStatus.values()) {
            statusCounts.put(status.name(), members.stream().filter(member -> member.getMemberStatus() == status).count());
        }
        Map<String, Long> genderCounts = new LinkedHashMap<>();
        for (Gender gender : Gender.values()) {
            genderCounts.put(gender.name(), active.stream().filter(member -> member.getGender() == gender).count());
        }
        genderCounts.put("UNKNOWN", active.stream().filter(member -> member.getGender() == null).count());

        LocalDate today = AppTime.today();
        Map<String, Long> ages = new LinkedHashMap<>();
        AGE_GROUPS.forEach(group -> ages.put(group, 0L));
        List<Integer> knownAges = new ArrayList<>();
        for (Member member : active) {
            Integer age = member.getDateOfBirth() == null ? null : Period.between(member.getDateOfBirth(), today).getYears();
            ages.merge(ageGroup(age), 1L, Long::sum);
            if (age != null) {
                knownAges.add(age);
            }
        }
        Double averageAge = knownAges.isEmpty() ? null : Math.round(knownAges.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0;

        return new MemberReport(
                members.size(),
                active.size(),
                statusCounts,
                genderCounts,
                ages.entrySet().stream().map(entry -> new Count(entry.getKey(), entry.getValue())).toList(),
                averageAge,
                byOrganization(members, target, currentUser)
        );
    }

    @Transactional(readOnly = true)
    public ParticipationReport participation(int year, PeriodGranularity granularity, String organizationId, CurrentUser currentUser) {
        if (year < 2000 || year > AppTime.today().getYear() + 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_YEAR", "Report year is out of range");
        }
        OrganizationUnit target = target(organizationId, currentUser);
        boolean tdpTarget = target != null && target.getType() == OrganizationUnitType.YOUTH_UNION_BRANCH;

        // invited members: active members of the report's scope
        List<Placement> placements = memberRoster.activePlacements().stream()
                .filter(placement -> !tdpTarget || target.getId().equals(placement.organizationId()))
                .toList();
        // events: those of the TDP and of its ward, or every event for a ward report
        List<Event> events = eventRepository.findAll(eventsOf(year, tdpTarget ? target : null)).stream()
                .sorted(Comparator.comparing(Event::getStartTime))
                .toList();
        Map<String, List<EventParticipation>> answers = events.isEmpty()
                ? Map.of()
                : participationRepository.findByEvent_IdIn(events.stream().map(Event::getId).toList()).stream()
                        .collect(Collectors.groupingBy(participation -> participation.getEvent().getId()));
        Map<String, OrganizationUnit> units = organizationUnitRepository.findAll().stream()
                .collect(Collectors.toMap(OrganizationUnit::getId, Function.identity()));

        Map<String, Tally> periods = new LinkedHashMap<>();
        int periodCount = granularity == PeriodGranularity.QUARTER ? 4 : 12;
        for (int i = 1; i <= periodCount; i++) {
            periods.put(periodKey(year, granularity, i), new Tally(periodKey(year, granularity, i), periodLabel(granularity, i)));
        }
        Tally total = new Tally("TOTAL", "Cả năm");
        Map<String, Tally> perOrganization = new LinkedHashMap<>();
        List<EventRow> eventRows = new ArrayList<>();

        for (Event event : events) {
            List<Placement> invited = placements.stream().filter(placement -> placement.in(event.getOrganizationId())).toList();
            Map<String, ParticipationStatus> statusByMember = answers.getOrDefault(event.getId(), List.of()).stream()
                    .collect(Collectors.toMap(EventParticipation::getMemberId, EventParticipation::getStatus, (left, right) -> left));
            ZonedDateTime start = event.getStartTime().atZone(AppTime.ZONE);
            int index = granularity == PeriodGranularity.QUARTER ? (start.getMonthValue() - 1) / 3 + 1 : start.getMonthValue();
            Tally period = periods.get(periodKey(year, granularity, index));
            period.eventCount++;
            total.eventCount++;
            long going = 0;
            for (Placement placement : invited) {
                ParticipationStatus status = statusByMember.get(placement.memberId());
                period.add(status);
                total.add(status);
                Tally unit = perOrganization.computeIfAbsent(placement.organizationId(), id -> new Tally(id, name(units, id)));
                unit.add(status);
                if (status == ParticipationStatus.GOING) {
                    going++;
                }
            }
            eventRows.add(new EventRow(
                    event.getId(),
                    event.getTitle(),
                    event.getStartTime(),
                    event.getOrganizationId(),
                    name(units, event.getOrganizationId()),
                    invited.size(),
                    going,
                    rate(going, invited.size())
            ));
        }

        return new ParticipationReport(
                year,
                granularity,
                periods.values().stream().map(Tally::toRow).toList(),
                total.toRow(),
                perOrganization.values().stream()
                        .sorted(Comparator.comparing(tally -> tally.label == null ? "" : tally.label))
                        .map(tally -> new OrganizationParticipation(tally.key, tally.label, tally.invited, tally.going, rate(tally.going, tally.invited)))
                        .toList(),
                eventRows
        );
    }

    /** null: the whole ward. TDP officers are always pinned to their own TDP. */
    private OrganizationUnit target(String organizationId, CurrentUser currentUser) {
        if (currentUser.isMember()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REPORT_FORBIDDEN", "Only officers can view reports");
        }
        String requested = organizationId == null || organizationId.isBlank() ? null : organizationId.trim();
        if (currentUser.hasTdpScope()) {
            if (requested != null && !OrgScopePolicy.isOwnTdp(currentUser, requested)) {
                throw OrgScopePolicy.outOfScope("Organization is outside current user's scope");
            }
            requested = currentUser.tdpId();
        }
        if (requested == null) {
            return null;
        }
        OrganizationUnit unit = organizationUnitRepository.findById(requested)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "Organization unit not found"));
        return unit.getType() == OrganizationUnitType.WARD ? null : unit;
    }

    private List<OrganizationMembers> byOrganization(List<Member> members, OrganizationUnit target, CurrentUser currentUser) {
        Map<String, OrganizationUnit> units = new LinkedHashMap<>();
        organizationUnitRepository.findByType(OrganizationUnitType.YOUTH_UNION_BRANCH).stream()
                .filter(unit -> target == null ? currentUser.hasWardScope() : unit.getId().equals(target.getId()))
                .sorted(Comparator.comparing(OrganizationUnit::getName))
                .forEach(unit -> units.put(unit.getId(), unit));
        members.forEach(member -> units.putIfAbsent(member.getOrganization().getId(), member.getOrganization()));
        Map<String, List<Member>> grouped = members.stream().collect(Collectors.groupingBy(member -> member.getOrganization().getId()));
        return units.values().stream().map(unit -> {
            List<Member> inUnit = grouped.getOrDefault(unit.getId(), List.of());
            List<Member> active = inUnit.stream().filter(member -> member.getMemberStatus() == MemberStatus.ACTIVE).toList();
            long male = active.stream().filter(member -> member.getGender() == Gender.MALE).count();
            long female = active.stream().filter(member -> member.getGender() == Gender.FEMALE).count();
            return new OrganizationMembers(unit.getId(), unit.getName(), inUnit.size(), active.size(), male, female, active.size() - male - female);
        }).toList();
    }

    private Specification<Event> eventsOf(int year, OrganizationUnit tdp) {
        return (root, query, cb) -> {
            var predicate = cb.and(
                    root.get("status").in(REPORTED_EVENT_STATUSES),
                    cb.greaterThanOrEqualTo(root.get("startTime"), AppTime.startOfDay(LocalDate.of(year, 1, 1))),
                    cb.lessThan(root.get("startTime"), AppTime.startOfDay(LocalDate.of(year + 1, 1, 1))));
            if (tdp == null) {
                return predicate;
            }
            List<String> organizations = new ArrayList<>(List.of(tdp.getId()));
            if (tdp.getParent() != null) {
                organizations.add(tdp.getParent().getId());
            }
            return cb.and(predicate, root.get("organizationId").in(organizations));
        };
    }

    private static boolean in(OrganizationUnit unit, String organizationId) {
        return unit.getId().equals(organizationId) || (unit.getParent() != null && unit.getParent().getId().equals(organizationId));
    }

    private static String ageGroup(Integer age) {
        if (age == null) {
            return "UNKNOWN";
        }
        if (age < 16) {
            return "UNDER_16";
        }
        if (age <= 20) {
            return "16_20";
        }
        if (age <= 25) {
            return "21_25";
        }
        return age <= 30 ? "26_30" : "OVER_30";
    }

    private static String periodKey(int year, PeriodGranularity granularity, int index) {
        return granularity == PeriodGranularity.QUARTER ? year + "-Q" + index : year + "-" + String.format("%02d", index);
    }

    private static String periodLabel(PeriodGranularity granularity, int index) {
        return (granularity == PeriodGranularity.QUARTER ? "Quý " : "Tháng ") + index;
    }

    private static String name(Map<String, OrganizationUnit> units, String id) {
        OrganizationUnit unit = id == null ? null : units.get(id);
        return unit == null ? null : unit.getName();
    }

    private static Double rate(long going, long invited) {
        return invited == 0 ? null : Math.round(going * 1000.0 / invited) / 1000.0;
    }

    /** Running totals of one period or organization. */
    private static final class Tally {

        private final String key;
        private final String label;
        private long eventCount;
        private long invited;
        private long going;
        private long notGoing;
        private long undecided;

        private Tally(String key, String label) {
            this.key = key;
            this.label = label;
        }

        private void add(ParticipationStatus status) {
            invited++;
            if (status == ParticipationStatus.GOING) {
                going++;
            } else if (status == ParticipationStatus.NOT_GOING) {
                notGoing++;
            } else if (status == ParticipationStatus.UNDECIDED) {
                undecided++;
            }
        }

        private ParticipationRow toRow() {
            return new ParticipationRow(key, label, eventCount, invited, going, notGoing, undecided, rate(going, invited));
        }
    }
}
