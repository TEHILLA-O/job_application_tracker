package com.victrom.jobtracker.service;

import com.victrom.jobtracker.dto.*;
import com.victrom.jobtracker.exception.BusinessRuleException;
import com.victrom.jobtracker.exception.NotFoundException;
import com.victrom.jobtracker.model.*;
import com.victrom.jobtracker.pipeline.PipelineRules;
import com.victrom.jobtracker.repository.ApplicationSpecifications;
import com.victrom.jobtracker.repository.JobApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobApplicationService {

    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> findAll(ApplicationFilter filter, Pageable pageable) {
        Page<ApplicationResponse> page = repository
                .findAll(ApplicationSpecifications.fromFilter(filter), pageable)
                .map(ApplicationMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse findById(Long id) {
        return ApplicationMapper.toDetail(loadDetailed(id));
    }

    public ApplicationDetailResponse create(CreateApplicationRequest request) {
        validateSalary(request.salaryMin(), request.salaryMax());
        JobApplication item = new JobApplication();
        apply(item, request);
        item.addEvent(new ApplicationEvent(
                EventType.CREATED,
                null,
                item.getStatus(),
                "Application recorded for " + item.getCompany()
        ));
        if (item.getNextFollowUp() != null) {
            item.addEvent(new ApplicationEvent(
                    EventType.FOLLOW_UP_SET, null, null,
                    "Follow-up set for " + item.getNextFollowUp()
            ));
        }
        return ApplicationMapper.toDetail(repository.save(item));
    }

    public ApplicationDetailResponse update(Long id, CreateApplicationRequest request) {
        validateSalary(request.salaryMin(), request.salaryMax());
        JobApplication item = loadDetailed(id);
        ApplicationStatus previous = item.getStatus();
        LocalDate previousFollowUp = item.getNextFollowUp();
        apply(item, request);
        if (previous != item.getStatus()) {
            PipelineRules.assertTransition(previous, item.getStatus());
            item.addEvent(new ApplicationEvent(
                    EventType.STATUS_CHANGED,
                    previous,
                    item.getStatus(),
                    "Status updated from " + previous + " to " + item.getStatus()
            ));
        } else {
            item.addEvent(new ApplicationEvent(EventType.UPDATED, previous, item.getStatus(), "Application details updated"));
        }
        if (!Objects.equals(previousFollowUp, item.getNextFollowUp()) && item.getNextFollowUp() != null) {
            item.addEvent(new ApplicationEvent(
                    EventType.FOLLOW_UP_SET, null, null,
                    "Follow-up set for " + item.getNextFollowUp()
            ));
        }
        return ApplicationMapper.toDetail(repository.save(item));
    }

    public ApplicationDetailResponse updateStatus(Long id, ApplicationStatus status) {
        JobApplication item = loadDetailed(id);
        ApplicationStatus previous = item.getStatus();
        PipelineRules.assertTransition(previous, status);
        if (previous != status) {
            item.setStatus(status);
            item.addEvent(new ApplicationEvent(
                    EventType.STATUS_CHANGED,
                    previous,
                    status,
                    "Moved from " + previous + " to " + status
            ));
        }
        return ApplicationMapper.toDetail(repository.save(item));
    }

    public ApplicationDetailResponse addNote(Long id, String message) {
        JobApplication item = loadDetailed(id);
        item.addEvent(new ApplicationEvent(EventType.NOTE_ADDED, item.getStatus(), item.getStatus(), message));
        if (item.getNotes() == null || item.getNotes().isBlank()) {
            item.setNotes(message);
        }
        return ApplicationMapper.toDetail(repository.save(item));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Application " + id + " was not found");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ApplicationStats stats() {
        List<JobApplication> items = repository.findAll();
        long total = items.size();
        long rejected = count(items, ApplicationStatus.REJECTED);
        long offers = count(items, ApplicationStatus.OFFER);
        long accepted = count(items, ApplicationStatus.ACCEPTED);
        long withdrawn = count(items, ApplicationStatus.WITHDRAWN);
        long interviews = items.stream().filter(a -> a.getStatus().isInterviewStage()).count();
        long active = items.stream().filter(a -> !a.getStatus().isClosed()).count();

        Map<String, Long> byStatus = enumCounts(items, a -> a.getStatus().name());
        Map<String, Long> bySource = enumCounts(items, a -> a.getSource().name());
        Map<String, Long> byPriority = enumCounts(items, a -> a.getPriority().name());

        double interviewRate = rate(interviews, total);
        double offerRate = rate(offers + accepted, total);
        double averageDaysOpen = items.isEmpty() ? 0 : Math.round(items.stream()
                .mapToLong(a -> ChronoUnit.DAYS.between(a.getAppliedDate(), LocalDate.now()))
                .average()
                .orElse(0) * 10.0) / 10.0;

        List<FunnelStep> funnel = List.of(
                new FunnelStep("SAVED", reached(items, ApplicationStatus.SAVED)),
                new FunnelStep("APPLIED", reached(items, ApplicationStatus.APPLIED)),
                new FunnelStep("ASSESSMENT", reached(items, ApplicationStatus.ASSESSMENT)),
                new FunnelStep("INTERVIEW", reached(items, ApplicationStatus.INTERVIEW)),
                new FunnelStep("OFFER", reached(items, ApplicationStatus.OFFER)),
                new FunnelStep("ACCEPTED", count(items, ApplicationStatus.ACCEPTED))
        );

        LocalDate today = LocalDate.now();
        List<FollowUpItem> followUps = items.stream()
                .filter(a -> a.getNextFollowUp() != null && !a.getStatus().isClosed())
                .sorted(Comparator.comparing(JobApplication::getNextFollowUp))
                .limit(8)
                .map(a -> new FollowUpItem(
                        a.getId(), a.getCompany(), a.getRole(), a.getNextFollowUp(),
                        !a.getNextFollowUp().isAfter(today)
                ))
                .toList();

        List<CompanyCount> topCompanies = items.stream()
                .collect(Collectors.groupingBy(JobApplication::getCompany, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new CompanyCount(entry.getKey(), entry.getValue()))
                .toList();

        return new ApplicationStats(
                total, active, offers, accepted, rejected, withdrawn,
                interviewRate, offerRate, averageDaysOpen,
                byStatus, bySource, byPriority,
                funnel, followUps, topCompanies
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("id,company,role,status,appliedDate,location,salaryMin,salaryMax,source,priority,nextFollowUp,tags,jobUrl\n");
        for (JobApplication item : repository.findAll()) {
            csv.append(item.getId()).append(',')
                    .append(csv(item.getCompany())).append(',')
                    .append(csv(item.getRole())).append(',')
                    .append(item.getStatus()).append(',')
                    .append(item.getAppliedDate()).append(',')
                    .append(csv(item.getLocation())).append(',')
                    .append(value(item.getSalaryMin())).append(',')
                    .append(value(item.getSalaryMax())).append(',')
                    .append(item.getSource()).append(',')
                    .append(item.getPriority()).append(',')
                    .append(value(item.getNextFollowUp())).append(',')
                    .append(csv(String.join("|", item.getTags()))).append(',')
                    .append(csv(item.getJobUrl()))
                    .append('\n');
        }
        return csv.toString();
    }

    private JobApplication loadDetailed(Long id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Application " + id + " was not found"));
    }

    private void apply(JobApplication item, CreateApplicationRequest request) {
        item.setCompany(request.company().trim());
        item.setRole(request.role().trim());
        item.setStatus(request.status());
        item.setAppliedDate(request.appliedDate());
        item.setLocation(blankToNull(request.location()));
        item.setSalaryMin(request.salaryMin());
        item.setSalaryMax(request.salaryMax());
        item.setNotes(blankToNull(request.notes()));
        item.setSource(request.source() == null ? ApplicationSource.OTHER : request.source());
        item.setJobUrl(blankToNull(request.jobUrl()));
        item.setRecruiterName(blankToNull(request.recruiterName()));
        item.setRecruiterEmail(blankToNull(request.recruiterEmail()));
        item.setPriority(request.priority() == null ? Priority.MEDIUM : request.priority());
        item.setNextFollowUp(request.nextFollowUp());
        item.getTags().clear();
        item.getTags().addAll(normalizeTags(request.tags()));
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private void validateSalary(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessRuleException("salaryMin cannot be greater than salaryMax");
        }
    }

    private long count(List<JobApplication> items, ApplicationStatus status) {
        return items.stream().filter(a -> a.getStatus() == status).count();
    }

    private long reached(List<JobApplication> items, ApplicationStatus stage) {
        return items.stream().filter(item -> reachedStage(item.getStatus(), stage)).count();
    }

    private boolean reachedStage(ApplicationStatus current, ApplicationStatus stage) {
        if (current == ApplicationStatus.REJECTED || current == ApplicationStatus.WITHDRAWN) {
            return stage.ordinal() <= ApplicationStatus.APPLIED.ordinal();
        }
        return current.ordinal() >= stage.ordinal();
    }

    private double rate(long part, long total) {
        if (total == 0) {
            return 0;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }

    private Map<String, Long> enumCounts(List<JobApplication> items,
                                         java.util.function.Function<JobApplication, String> key) {
        return items.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.counting()));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
