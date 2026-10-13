package com.dotani.poll.service;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditDiff;
import com.dotani.audit.service.AuditLogService;
import com.dotani.event.repository.EventRepository;
import com.dotani.member.service.MemberRoster;
import com.dotani.member.service.NameLookup;
import com.dotani.member.service.OrganizationDirectory;
import com.dotani.poll.dto.PollDtos.OptionResponse;
import com.dotani.poll.dto.PollDtos.PollRequest;
import com.dotani.poll.dto.PollDtos.PollResponse;
import com.dotani.poll.dto.PollDtos.Voter;
import com.dotani.poll.entity.Poll;
import com.dotani.poll.entity.PollAnswer;
import com.dotani.poll.entity.PollBallot;
import com.dotani.poll.entity.PollOption;
import com.dotani.poll.entity.PollStatus;
import com.dotani.poll.entity.ResultsVisibility;
import com.dotani.poll.repository.PollAnswerRepository;
import com.dotani.poll.repository.PollBallotRepository;
import com.dotani.poll.repository.PollRepository;
import com.dotani.shared.events.DomainEvents.PollOpened;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khảo sát / biểu quyết. Officers manage polls of their scope (ward officers everywhere, TDP officers in
 * their TDP); the active members of the poll's organization vote once. In a secret ballot the choices are
 * stored without the voter, so nobody (officers included) can see who chose what.
 */
@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollBallotRepository ballotRepository;
    private final PollAnswerRepository answerRepository;
    private final EventRepository eventRepository;
    private final MemberRoster memberRoster;
    private final NameLookup nameLookup;
    private final OrganizationDirectory organizationDirectory;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher events;

    public PollService(
            PollRepository pollRepository,
            PollBallotRepository ballotRepository,
            PollAnswerRepository answerRepository,
            EventRepository eventRepository,
            MemberRoster memberRoster,
            NameLookup nameLookup,
            OrganizationDirectory organizationDirectory,
            AuditLogService auditLogService,
            ApplicationEventPublisher events
    ) {
        this.pollRepository = pollRepository;
        this.ballotRepository = ballotRepository;
        this.answerRepository = answerRepository;
        this.eventRepository = eventRepository;
        this.memberRoster = memberRoster;
        this.nameLookup = nameLookup;
        this.organizationDirectory = organizationDirectory;
        this.auditLogService = auditLogService;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<PollResponse> findAll(PollStatus status, String eventId, Pageable pageable, CurrentUser currentUser) {
        Specification<Poll> specification = Specification
                .where(PollRepository.visibleTo(currentUser))
                .and(PollRepository.hasStatus(status))
                .and(PollRepository.ofEvent(eventId));
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Poll> page = pollRepository.findAll(specification, sorted);
        Context context = context(page.getContent(), currentUser);
        return page.map(poll -> toResponse(poll, context, currentUser));
    }

    @Transactional(readOnly = true)
    public PollResponse findById(String id, CurrentUser currentUser) {
        Poll poll = getReadablePoll(id, currentUser);
        return toResponse(poll, context(List.of(poll), currentUser), currentUser);
    }

    @Transactional
    public PollResponse create(PollRequest request, CurrentUser currentUser) {
        requireManage(currentUser, request.organizationId());
        Poll poll = new Poll();
        apply(poll, request);
        poll.setAnonymous(request.anonymous());
        poll.setMaxChoices(request.maxChoices());
        poll.replaceOptions(labels(request));
        poll.setCreatedBy(currentUser.memberId());
        Poll saved = pollRepository.save(poll);
        auditLogService.record(currentUser, AuditAction.CREATE_POLL, ResourceType.POLL, saved.getId(),
                saved.getOrganizationId(), AuditDiff.created(auditSnapshot(saved)));
        if (Boolean.TRUE.equals(request.open())) {
            open(saved, currentUser);
        }
        return findById(saved.getId(), currentUser);
    }

    @Transactional
    public PollResponse update(String id, PollRequest request, CurrentUser currentUser) {
        Poll poll = getManagedPoll(id, currentUser);
        requireManage(currentUser, request.organizationId());
        Map<String, Object> before = auditSnapshot(poll);
        List<String> labels = labels(request);
        if (poll.getStatus() == PollStatus.DRAFT) {
            poll.setAnonymous(request.anonymous());
            poll.setMaxChoices(request.maxChoices());
            if (!labels.equals(currentLabels(poll))) {
                poll.replaceOptions(labels);
            }
        } else if (poll.isAnonymous() != request.anonymous()
                || poll.getMaxChoices() != request.maxChoices()
                || !labels.equals(currentLabels(poll))
                || !poll.getOrganizationId().equals(request.organizationId().trim())) {
            // votes were cast against these options and this electorate
            throw new ApiException(HttpStatus.CONFLICT, "POLL_LOCKED", "Options and voting rules are locked once the poll opened");
        }
        apply(poll, request);
        auditLogService.record(currentUser, AuditAction.UPDATE_POLL, ResourceType.POLL, poll.getId(),
                poll.getOrganizationId(), AuditDiff.changes(before, auditSnapshot(poll)));
        return findById(id, currentUser);
    }

    @Transactional
    public PollResponse open(String id, CurrentUser currentUser) {
        Poll poll = getManagedPoll(id, currentUser);
        if (poll.getStatus() != PollStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_POLL_STATUS", "Only a draft can be opened");
        }
        open(poll, currentUser);
        return findById(id, currentUser);
    }

    @Transactional
    public PollResponse close(String id, CurrentUser currentUser) {
        Poll poll = getManagedPoll(id, currentUser);
        if (poll.getStatus() != PollStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_POLL_STATUS", "Only an open poll can be closed");
        }
        poll.close(Instant.now());
        auditLogService.record(currentUser, AuditAction.CLOSE_POLL, ResourceType.POLL, poll.getId(), poll.getOrganizationId());
        return findById(id, currentUser);
    }

    @Transactional
    public void delete(String id, CurrentUser currentUser) {
        Poll poll = getManagedPoll(id, currentUser);
        auditLogService.record(currentUser, AuditAction.DELETE_POLL, ResourceType.POLL, poll.getId(),
                poll.getOrganizationId(), AuditDiff.deleted(auditSnapshot(poll)));
        poll.markDeleted();
    }

    @Transactional
    public PollResponse vote(String id, List<String> optionIds, CurrentUser currentUser) {
        getReadablePoll(id, currentUser);
        // lock the poll row: two votes of the same member cannot both pass the "already voted" check
        Poll poll = pollRepository.findByIdForUpdate(id).orElseThrow();
        Instant now = Instant.now();
        if (poll.effectiveStatus(now) != PollStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "POLL_NOT_OPEN", "Poll is not open");
        }
        String memberId = currentUser.memberId();
        if (!memberRoster.isActiveIn(memberId, poll.getOrganizationId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "POLL_NOT_ELIGIBLE", "Caller is not an active member of the poll's organization");
        }
        if (ballotRepository.existsById(new PollBallot.Key(id, memberId))) {
            throw new ApiException(HttpStatus.CONFLICT, "POLL_ALREADY_VOTED", "Member already voted");
        }
        Set<String> chosen = new LinkedHashSet<>(optionIds);
        Set<String> valid = poll.getOptions().stream().map(PollOption::getId).collect(Collectors.toSet());
        if (chosen.size() != optionIds.size() || chosen.size() > poll.getMaxChoices() || !valid.containsAll(chosen)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_POLL_OPTIONS", "Invalid choice", poll.getMaxChoices());
        }
        ballotRepository.save(new PollBallot(id, memberId, now));
        chosen.forEach(optionId -> answerRepository.save(new PollAnswer(id, optionId, poll.isAnonymous() ? null : memberId)));
        answerRepository.flush();
        return findById(id, currentUser);
    }

    /** Closes polls whose closing time passed. */
    @Scheduled(cron = "${app.polls.close-cron:0 */5 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int closeExpired() {
        Instant now = Instant.now();
        List<Poll> expired = pollRepository.findByStatusAndClosesAtLessThanEqual(PollStatus.OPEN, now);
        expired.forEach(poll -> poll.close(poll.getClosesAt()));
        return expired.size();
    }

    private void open(Poll poll, CurrentUser currentUser) {
        Instant now = Instant.now();
        if (poll.getClosesAt() != null && !poll.getClosesAt().isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_POLL_CLOSE_TIME", "Closing time must be in the future");
        }
        poll.open(now);
        auditLogService.record(currentUser, AuditAction.OPEN_POLL, ResourceType.POLL, poll.getId(), poll.getOrganizationId());
        events.publishEvent(new PollOpened(poll.getId(), poll.getTitle(), poll.getOrganizationId()));
    }

    private void apply(Poll poll, PollRequest request) {
        String organizationId = request.organizationId().trim();
        organizationDirectory.requireExists(organizationId);
        String eventId = blankToNull(request.eventId());
        if (eventId != null && !eventRepository.existsById(eventId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Event not found");
        }
        if (request.closesAt() != null && poll.getStatus() != PollStatus.CLOSED && !request.closesAt().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_POLL_CLOSE_TIME", "Closing time must be in the future");
        }
        poll.setTitle(request.title().trim());
        poll.setDescription(blankToNull(request.description()));
        poll.setOrganizationId(organizationId);
        poll.setEventId(eventId);
        poll.setResultsVisibility(request.resultsVisibility());
        poll.setClosesAt(request.closesAt());
    }

    private static List<String> labels(PollRequest request) {
        return request.options().stream().map(String::trim).toList();
    }

    private static List<String> currentLabels(Poll poll) {
        return poll.getOptions().stream().map(PollOption::getLabel).toList();
    }

    /** Data shared by the responses of one request, loaded once. */
    private record Context(
            Map<String, List<String>> eligibleByOrganization,
            Map<String, Long> ballots,
            Set<String> votedPollIds,
            NameLookup.Names names
    ) {
    }

    private Context context(List<Poll> polls, CurrentUser currentUser) {
        List<String> pollIds = polls.stream().map(Poll::getId).toList();
        Map<String, List<String>> eligible = new HashMap<>();
        polls.forEach(poll -> eligible.computeIfAbsent(poll.getOrganizationId(), memberRoster::activeMemberIdsIn));
        Set<String> voted = currentUser.memberId() == null || pollIds.isEmpty()
                ? Set.of()
                : new HashSet<>(ballotRepository.findPollIdsVotedBy(pollIds, currentUser.memberId()));
        NameLookup.Names names = nameLookup.of(
                polls.stream().map(Poll::getCreatedBy).toList(),
                polls.stream().map(Poll::getOrganizationId).toList());
        return new Context(eligible, ballotRepository.counts(pollIds), voted, names);
    }

    private PollResponse toResponse(Poll poll, Context context, CurrentUser currentUser) {
        Instant now = Instant.now();
        PollStatus status = poll.effectiveStatus(now);
        boolean canManage = canManage(currentUser, poll.getOrganizationId());
        boolean hasVoted = context.votedPollIds().contains(poll.getId());
        List<String> eligibleIds = context.eligibleByOrganization().getOrDefault(poll.getOrganizationId(), List.of());
        boolean resultsVisible = canManage
                || poll.getResultsVisibility() == ResultsVisibility.ALWAYS
                || status == PollStatus.CLOSED
                || (poll.getResultsVisibility() == ResultsVisibility.AFTER_VOTE && hasVoted);

        Map<String, Long> counts = new HashMap<>();
        Map<String, List<Voter>> voters = new HashMap<>();
        List<String> myOptionIds = List.of();
        if (status != PollStatus.DRAFT && (resultsVisible || (hasVoted && !poll.isAnonymous()))) {
            List<PollAnswer> answers = answerRepository.findByPollId(poll.getId());
            answers.forEach(answer -> counts.merge(answer.getOptionId(), 1L, Long::sum));
            if (!poll.isAnonymous()) {
                myOptionIds = answers.stream()
                        .filter(answer -> answer.getMemberId() != null && answer.getMemberId().equals(currentUser.memberId()))
                        .map(PollAnswer::getOptionId)
                        .toList();
                if (canManage) {
                    NameLookup.Names voterNames = nameLookup.of(answers.stream().map(PollAnswer::getMemberId).toList(), List.of());
                    answers.forEach(answer -> voters.computeIfAbsent(answer.getOptionId(), key -> new ArrayList<>())
                            .add(new Voter(answer.getMemberId(), voterNames.member(answer.getMemberId()))));
                }
            }
        }
        List<OptionResponse> options = poll.getOptions().stream()
                .map(option -> new OptionResponse(
                        option.getId(),
                        option.getLabel(),
                        option.getPosition(),
                        resultsVisible ? counts.getOrDefault(option.getId(), 0L) : null,
                        canManage && !poll.isAnonymous() ? voters.getOrDefault(option.getId(), List.of()) : null))
                .toList();

        return new PollResponse(
                poll.getId(),
                poll.getTitle(),
                poll.getDescription(),
                poll.getOrganizationId(),
                context.names().organization(poll.getOrganizationId()),
                poll.getEventId(),
                poll.isAnonymous(),
                poll.getMaxChoices(),
                poll.getResultsVisibility(),
                status,
                poll.getClosesAt(),
                poll.getOpenedAt(),
                poll.getClosedAt(),
                poll.getCreatedBy(),
                context.names().member(poll.getCreatedBy()),
                poll.getCreatedAt(),
                options,
                eligibleIds.size(),
                context.ballots().getOrDefault(poll.getId(), 0L),
                currentUser.memberId() != null && eligibleIds.contains(currentUser.memberId()),
                hasVoted,
                myOptionIds,
                resultsVisible,
                canManage
        );
    }

    private Poll getReadablePoll(String id, CurrentUser currentUser) {
        Poll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POLL_NOT_FOUND", "Poll not found"));
        if (!OrgScopePolicy.canSeeContentOf(currentUser, poll.getOrganizationId())) {
            throw OrgScopePolicy.outOfScope("Poll is outside current user's scope");
        }
        if (currentUser.isMember() && poll.getStatus() == PollStatus.DRAFT) {
            throw new ApiException(HttpStatus.NOT_FOUND, "POLL_NOT_FOUND", "Poll not found");
        }
        return poll;
    }

    private Poll getManagedPoll(String id, CurrentUser currentUser) {
        Poll poll = getReadablePoll(id, currentUser);
        requireManage(currentUser, poll.getOrganizationId());
        return poll;
    }

    private static boolean canManage(CurrentUser currentUser, String organizationId) {
        return currentUser.hasWardScope() || (currentUser.hasTdpScope() && OrgScopePolicy.isOwnTdp(currentUser, organizationId));
    }

    private static void requireManage(CurrentUser currentUser, String organizationId) {
        if (!canManage(currentUser, organizationId == null ? null : organizationId.trim())) {
            throw OrgScopePolicy.outOfScope("Poll is outside current user's scope");
        }
    }

    private static Map<String, Object> auditSnapshot(Poll poll) {
        return AuditDiff.snapshot(
                "title", poll.getTitle(),
                "organizationId", poll.getOrganizationId(),
                "eventId", poll.getEventId(),
                "anonymous", poll.isAnonymous(),
                "maxChoices", poll.getMaxChoices(),
                "closesAt", poll.getClosesAt(),
                "options", String.join(" | ", currentLabels(poll))
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
