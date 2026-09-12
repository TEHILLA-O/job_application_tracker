package com.victrom.jobtracker.config;

import com.victrom.jobtracker.dto.CreateApplicationRequest;
import com.victrom.jobtracker.model.ApplicationSource;
import com.victrom.jobtracker.model.ApplicationStatus;
import com.victrom.jobtracker.model.Priority;
import com.victrom.jobtracker.repository.JobApplicationRepository;
import com.victrom.jobtracker.service.JobApplicationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
@Profile("!test")
public class SampleDataConfig {

    @Bean
    CommandLineRunner seedApplications(JobApplicationService service, JobApplicationRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            Long northstar = create(service, "Northstar Digital", "Application Support Analyst",
                    ApplicationStatus.APPLIED, 18, "London", "48000", "56000",
                    "Applied through the careers portal.", ApplicationSource.COMPANY_SITE,
                    "https://northstar.example/jobs/support", "Priya Shah", "priya@northstar.example",
                    Priority.HIGH, 2, List.of("support", "java"));
            service.updateStatus(northstar, ApplicationStatus.ASSESSMENT);
            service.updateStatus(northstar, ApplicationStatus.INTERVIEW);
            service.addNote(northstar, "Second-stage interview booked for next week.");

            Long civic = create(service, "Civic Systems", "Junior Data Analyst",
                    ApplicationStatus.APPLIED, 6, "Hybrid · Manchester", "36000", "42000",
                    "Submitted via company site after a meetup.", ApplicationSource.COMPANY_SITE,
                    null, null, null, Priority.MEDIUM, 1, List.of("sql", "python"));
            service.addNote(civic, "Recruiter acknowledged the application.");

            create(service, "Harbour & Co", "Graduate Software Engineer",
                    ApplicationStatus.SAVED, 2, "Remote", "40000", "48000",
                    "Need to tailor the cover letter before applying.", ApplicationSource.LINKEDIN,
                    "https://linkedin.com/jobs/harbour", null, null,
                    Priority.MEDIUM, 0, List.of("java", "spring"));

            Long atlas = create(service, "Atlas Retail", "IT Service Desk Analyst",
                    ApplicationStatus.APPLIED, 21, "Birmingham", "30000", "34000",
                    "Agency-led application.", ApplicationSource.RECRUITER,
                    null, "James Cole", "james@atlas.example", Priority.LOW, -1,
                    List.of("itil"));
            service.updateStatus(atlas, ApplicationStatus.REJECTED);
            service.addNote(atlas, "Rejected after screening. Feedback: wants more desktop support hours.");

            Long lumen = create(service, "Lumen Health", "Backend Developer",
                    ApplicationStatus.APPLIED, 12, "London", "52000", "62000",
                    "Strong overlap with Spring Boot and APIs.", ApplicationSource.LINKEDIN,
                    null, "Amelia Hart", "amelia@lumen.example", Priority.HIGH, 3,
                    List.of("java", "api"));
            service.updateStatus(lumen, ApplicationStatus.ASSESSMENT);
            service.addNote(lumen, "Take-home is a small REST service. Due in three days.");

            Long ridge = create(service, "Ridgeway Bank", "Junior Java Developer",
                    ApplicationStatus.APPLIED, 27, "Canary Wharf", "45000", "55000",
                    "Employee referral from a former classmate.", ApplicationSource.REFERRAL,
                    null, null, null, Priority.HIGH, 4, List.of("java", "sql"));
            service.updateStatus(ridge, ApplicationStatus.ASSESSMENT);
            service.updateStatus(ridge, ApplicationStatus.INTERVIEW);
            service.updateStatus(ridge, ApplicationStatus.OFFER);
            service.addNote(ridge, "Verbal offer received. Waiting for written package.");

            create(service, "Oak & Pine", "Product Analyst",
                    ApplicationStatus.WITHDRAWN, 30, "Bristol", "38000", "44000",
                    "Withdrew after accepting another interview loop.", ApplicationSource.INDEED,
                    null, null, null, Priority.LOW, null, List.of("analytics"));

            create(service, "Nimbus Labs", "Platform Engineer",
                    ApplicationStatus.APPLIED, 4, "Remote", "50000", "65000",
                    "GitHub-sourced listing. Mentioned homelab projects.", ApplicationSource.GITHUB,
                    "https://github.com/jobs/nimbus", null, null, Priority.MEDIUM, 5,
                    List.of("docker", "linux"));
        };
    }

    private Long create(JobApplicationService service,
                        String company,
                        String role,
                        ApplicationStatus status,
                        int daysAgo,
                        String location,
                        String salaryMin,
                        String salaryMax,
                        String notes,
                        ApplicationSource source,
                        String jobUrl,
                        String recruiterName,
                        String recruiterEmail,
                        Priority priority,
                        Integer followUpInDays,
                        List<String> tags) {
        LocalDate applied = LocalDate.now().minusDays(daysAgo);
        LocalDate followUp = followUpInDays == null ? null : LocalDate.now().plusDays(followUpInDays);
        return service.create(new CreateApplicationRequest(
                company, role, status, applied, location,
                new BigDecimal(salaryMin), new BigDecimal(salaryMax), notes,
                source, jobUrl, recruiterName, recruiterEmail, priority, followUp, tags
        )).application().id();
    }
}
