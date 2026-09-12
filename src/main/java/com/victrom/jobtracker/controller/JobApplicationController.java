package com.victrom.jobtracker.controller;

import com.victrom.jobtracker.dto.AddNoteRequest;
import com.victrom.jobtracker.dto.ApplicationDetailResponse;
import com.victrom.jobtracker.dto.ApplicationFilter;
import com.victrom.jobtracker.dto.ApplicationResponse;
import com.victrom.jobtracker.dto.ApplicationStats;
import com.victrom.jobtracker.dto.CreateApplicationRequest;
import com.victrom.jobtracker.dto.PagedResponse;
import com.victrom.jobtracker.dto.UpdateStatusRequest;
import com.victrom.jobtracker.model.ApplicationSource;
import com.victrom.jobtracker.model.ApplicationStatus;
import com.victrom.jobtracker.model.Priority;
import com.victrom.jobtracker.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Job application pipeline, notes, analytics and export")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Search and page applications")
    public PagedResponse<ApplicationResponse> all(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ApplicationSource source,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Boolean followUpDue,
            @PageableDefault(size = 50, sort = "appliedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.findAll(new ApplicationFilter(status, company, q, source, priority, followUpDue), pageable);
    }

    @GetMapping("/stats")
    @Operation(summary = "Pipeline analytics")
    public ApplicationStats stats() {
        return service.stats();
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @Operation(summary = "Export the pipeline as CSV")
    public ResponseEntity<String> exportCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"applications.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(service.exportCsv());
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get one application with its timeline")
    public ApplicationDetailResponse one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create an application")
    public ResponseEntity<ApplicationDetailResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Replace an application")
    public ApplicationDetailResponse update(@PathVariable Long id,
                                            @Valid @RequestBody CreateApplicationRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id:\\d+}/status")
    @Operation(summary = "Move an application to another pipeline stage")
    public ApplicationDetailResponse updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request.status());
    }

    @PostMapping("/{id:\\d+}/notes")
    @Operation(summary = "Add a timeline note")
    public ApplicationDetailResponse addNote(@PathVariable Long id,
                                             @Valid @RequestBody AddNoteRequest request) {
        return service.addNote(id, request.message());
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an application")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
