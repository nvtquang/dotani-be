package com.hcmcyu.content.config;

import com.hcmcyu.content.entity.Post;
import com.hcmcyu.content.entity.PostImage;
import com.hcmcyu.content.entity.PostStatus;
import com.hcmcyu.content.entity.PostType;
import com.hcmcyu.content.repository.PostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final PostRepository postRepository;

    public DevDataSeeder(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public void run(String... args) {
        if (postRepository.count() > 0) {
            return;
        }

        seedPost(
                "Thông báo lịch sinh hoạt Đoàn phường",
                "Lịch sinh hoạt và triển khai công việc tháng này.",
                PostType.ANNOUNCEMENT,
                "ward-thuong-cat",
                "ward-secretary-member",
                "dev-ward-announcement.png"
        );
        seedPost(
                "Tin mới từ Đoàn phường Thượng Cát",
                "Cập nhật các hoạt động nổi bật của Đoàn phường.",
                PostType.NEWS,
                "ward-thuong-cat",
                "ward-deputy-member",
                "dev-ward-news.png"
        );

        for (int tdp = 1; tdp <= 5; tdp++) {
            seedPost(
                    "Báo cáo hoạt động TDP " + tdp,
                    "Báo cáo ảnh hoạt động tình nguyện của chi đoàn TDP " + tdp + ".",
                    PostType.ACTIVITY_REPORT,
                    "tdp-" + tdp,
                    "tdp-" + tdp + "-secretary-member",
                    "dev-tdp-" + tdp + "-report.png"
            );
            seedPost(
                    "Thông tin sinh hoạt TDP " + tdp,
                    "Nội dung sinh hoạt chi đoàn TDP " + tdp + ".",
                    PostType.OTHER,
                    "tdp-" + tdp,
                    "tdp-" + tdp + "-deputy-member",
                    "dev-tdp-" + tdp + "-activity.webp"
            );
        }
    }

    private void seedPost(
            String title,
            String content,
            PostType type,
            String organizationId,
            String authorId,
            String imageName
    ) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setType(type);
        post.setOrganizationId(organizationId);
        post.setAuthorId(authorId);
        post.setStatus(PostStatus.PUBLISHED);

        PostImage image = new PostImage();
        image.setImageUrl("/uploads/post-images/" + imageName);
        post.addImage(image);
        postRepository.save(post);
    }
}
