package com.example.resourcesharingplatform.config;

import com.example.resourcesharingplatform.entity.Category;
import com.example.resourcesharingplatform.entity.CompetitionType;
import com.example.resourcesharingplatform.entity.User;
import com.example.resourcesharingplatform.repository.CategoryRepository;
import com.example.resourcesharingplatform.repository.CompetitionTypeRepository;
import com.example.resourcesharingplatform.repository.UserRepository;
import com.example.resourcesharingplatform.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CompetitionTypeRepository competitionTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
        initCategories();
        initCompetitionTypes();
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@example.com")
                    .phone("13800138000")
                    .realName("系统管理员")
                    .role(Constants.ROLE_ADMIN)
                    .status(Constants.USER_STATUS_ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("管理员账号创建成功: admin / admin123");
        }
    }

    private void initCategories() {
        if (categoryRepository.count() == 0) {
            Category doc = Category.builder()
                    .name("文档资料")
                    .description("各类文档资料")
                    .sortOrder(1)
                    .status(1)
                    .build();
            categoryRepository.save(doc);

            Category video = Category.builder()
                    .name("视频资料")
                    .description("各类视频资料")
                    .sortOrder(2)
                    .status(1)
                    .build();
            categoryRepository.save(video);

            Category image = Category.builder()
                    .name("图片资料")
                    .description("各类图片资料")
                    .sortOrder(3)
                    .status(1)
                    .build();
            categoryRepository.save(image);

            Category template = Category.builder()
                    .name("模板素材")
                    .description("各类模板素材")
                    .sortOrder(4)
                    .status(1)
                    .build();
            categoryRepository.save(template);

            Category other = Category.builder()
                    .name("其他资料")
                    .description("其他类型资料")
                    .sortOrder(5)
                    .status(1)
                    .build();
            categoryRepository.save(other);

            log.info("分类数据初始化完成");
        }
    }

    private void initCompetitionTypes() {
        if (competitionTypeRepository.count() == 0) {
            CompetitionType programming = CompetitionType.builder()
                    .name("程序设计竞赛")
                    .description("ACM、ICPC等程序设计竞赛")
                    .sortOrder(1)
                    .status(1)
                    .build();
            competitionTypeRepository.save(programming);

            CompetitionType algorithm = CompetitionType.builder()
                    .name("算法竞赛")
                    .description("蓝桥杯、Kaggle等算法竞赛")
                    .sortOrder(2)
                    .status(1)
                    .build();
            competitionTypeRepository.save(algorithm);

            CompetitionType innovation = CompetitionType.builder()
                    .name("创新创业竞赛")
                    .description("互联网+、挑战杯等创新创业竞赛")
                    .sortOrder(3)
                    .status(1)
                    .build();
            competitionTypeRepository.save(innovation);

            CompetitionType math = CompetitionType.builder()
                    .name("数学建模竞赛")
                    .description("MathorCup、美赛等数学建模竞赛")
                    .sortOrder(4)
                    .status(1)
                    .build();
            competitionTypeRepository.save(math);

            CompetitionType other = CompetitionType.builder()
                    .name("其他竞赛")
                    .description("其他类型的竞赛")
                    .sortOrder(5)
                    .status(1)
                    .build();
            competitionTypeRepository.save(other);

            log.info("竞赛类型数据初始化完成");
        }
    }
}