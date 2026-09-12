package com.victrom.jobtracker.pipeline;

import com.victrom.jobtracker.exception.InvalidTransitionException;
import com.victrom.jobtracker.model.ApplicationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PipelineRulesTest {

    @Test
    void allowsAppliedToInterviewViaAssessment() {
        assertDoesNotThrow(() -> PipelineRules.assertTransition(ApplicationStatus.APPLIED, ApplicationStatus.ASSESSMENT));
        assertDoesNotThrow(() -> PipelineRules.assertTransition(ApplicationStatus.ASSESSMENT, ApplicationStatus.INTERVIEW));
    }

    @Test
    void rejectsJumpFromSavedToOffer() {
        assertThrows(InvalidTransitionException.class,
                () -> PipelineRules.assertTransition(ApplicationStatus.SAVED, ApplicationStatus.OFFER));
    }

    @Test
    void acceptedIsTerminal() {
        assertThrows(InvalidTransitionException.class,
                () -> PipelineRules.assertTransition(ApplicationStatus.ACCEPTED, ApplicationStatus.APPLIED));
    }
}
