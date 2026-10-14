package experiment;

import config.PaymentTimingConfig;
import org.junit.jupiter.api.Test;
import timing.TransitionTimingConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [T4.2.2 - CAMPAIGN PLAN TESTS]
 *
 * Verifica que la campaña formal se construya exactamente
 * según docs/protocolo-experimentos.md.
 */
class CampaignPlanTest {

    @Test
    void shouldCreateExactlyTwentyFormalRuns() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        assertEquals(
                20,
                configs.size()
        );
    }

    @Test
    void shouldCreateFiveRunsForEachPolicyAndTimingCombination() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        assertEquals(
                5,
                count(
                        configs,
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        "BASELINE"
                )
        );

        assertEquals(
                5,
                count(
                        configs,
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        "VARIANT"
                )
        );

        assertEquals(
                5,
                count(
                        configs,
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        "BASELINE"
                )
        );

        assertEquals(
                5,
                count(
                        configs,
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        "VARIANT"
                )
        );
    }

    @Test
    void shouldUseOfficialSeedsForEveryCombination() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        List<Long> expectedSeeds =
                List.of(
                        2026L,
                        2027L,
                        2028L,
                        2029L,
                        2030L
                );

        for (ExperimentRunConfig.PolicyMode policy
                : ExperimentRunConfig.PolicyMode.values()) {

            for (String timing
                    : List.of(
                            "BASELINE",
                            "VARIANT"
                    )) {

                List<Long> actualSeeds =
                        configs.stream()
                                .filter(
                                        config ->
                                                config.getPolicyMode()
                                                        == policy
                                )
                                .filter(
                                        config ->
                                                config.getTimingName()
                                                        .equals(timing)
                                )
                                .map(
                                        ExperimentRunConfig::getSeed
                                )
                                .toList();

                assertEquals(
                        expectedSeeds,
                        actualSeeds
                );
            }
        }
    }

    @Test
    void shouldCreateUniqueRunIdsAndLogPaths() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        Set<String> runIds =
                new HashSet<>();

        Set<Path> logPaths =
                new HashSet<>();

        for (ExperimentRunConfig config
                : configs) {

            assertTrue(
                    runIds.add(
                            config.getRunId()
                    ),
                    "Duplicate runId: "
                            + config.getRunId()
            );

            assertTrue(
                    logPaths.add(
                            config.getLogPath()
                    ),
                    "Duplicate log path: "
                            + config.getLogPath()
            );
        }

        assertEquals(
                20,
                runIds.size()
        );

        assertEquals(
                20,
                logPaths.size()
        );
    }

    @Test
    void shouldUseTargetAndTimeoutsDefinedForFormalCampaign() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        for (ExperimentRunConfig config
                : configs) {

            assertEquals(
                    200,
                    config.getTarget()
            );

            assertEquals(
                    Duration.ofSeconds(60),
                    config.getExecutionTimeout()
            );

            assertEquals(
                    Duration.ofSeconds(5),
                    config.getShutdownTimeout()
            );
        }
    }

    @Test
    void shouldFollowProtocolOrdering() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        assertEquals(
                "RANDOM_BASELINE_2026",
                configs.get(0).getRunId()
        );

        assertEquals(
                "RANDOM_BASELINE_2030",
                configs.get(4).getRunId()
        );

        assertEquals(
                "RANDOM_VARIANT_2026",
                configs.get(5).getRunId()
        );

        assertEquals(
                "RANDOM_VARIANT_2030",
                configs.get(9).getRunId()
        );

        assertEquals(
                "PRIORITY_BASELINE_2026",
                configs.get(10).getRunId()
        );

        assertEquals(
                "PRIORITY_BASELINE_2030",
                configs.get(14).getRunId()
        );

        assertEquals(
                "PRIORITY_VARIANT_2026",
                configs.get(15).getRunId()
        );

        assertEquals(
                "PRIORITY_VARIANT_2030",
                configs.get(19).getRunId()
        );
    }

    @Test
    void shouldUseCalibratedVariantForEveryVariantRun() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        List<ExperimentRunConfig> variantRuns =
                configs.stream()
                        .filter(
                                config ->
                                        config.getTimingName()
                                                .equals("VARIANT")
                        )
                        .toList();

        assertEquals(
                10,
                variantRuns.size()
        );

        for (ExperimentRunConfig config
                : variantRuns) {

            assertSame(
                    variant,
                    config.getTimingConfig()
            );
        }
    }

    @Test
    void shouldUseBaselineTimingForBaselineRuns() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        for (ExperimentRunConfig config
                : configs) {

            if (!config.getTimingName()
                    .equals("BASELINE")) {

                continue;
            }

            TransitionTimingConfig timing =
                    config.getTimingConfig();

            assertEquals(
                    120L,
                    timing.getDelayMillis(2)
            );

            assertEquals(
                    80L,
                    timing.getDelayMillis(3)
            );

            assertEquals(
                    150L,
                    timing.getDelayMillis(5)
            );

            assertEquals(
                    100L,
                    timing.getDelayMillis(7)
            );

            assertEquals(
                    120L,
                    timing.getDelayMillis(8)
            );
        }
    }

    @Test
    void shouldCreateProtocolLogPaths() {

        TransitionTimingConfig variant =
                PaymentTimingConfig.createScaled(
                        1.20
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variant
                );

        ExperimentRunConfig first =
                configs.get(0);

        assertEquals(
                Path.of(
                        "results",
                        "logs",
                        "RANDOM_BASELINE_2026.log"
                ),
                first.getLogPath()
        );

        ExperimentRunConfig last =
                configs.get(19);

        assertEquals(
                Path.of(
                        "results",
                        "logs",
                        "PRIORITY_VARIANT_2030.log"
                ),
                last.getLogPath()
        );
    }

    @Test
    void shouldRejectNullVariantTiming() {

        assertThrows(
                NullPointerException.class,
                () ->
                        CampaignPlan.createFormalCampaign(
                                null
                        )
        );
    }

    private long count(
            List<ExperimentRunConfig> configs,
            ExperimentRunConfig.PolicyMode policy,
            String timing
    ) {

        return configs.stream()
                .filter(
                        config ->
                                config.getPolicyMode()
                                        == policy
                )
                .filter(
                        config ->
                                config.getTimingName()
                                        .equals(timing)
                )
                .count();
    }
}
