package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Tattered Ratter. */
class TatteredRatterScenarioTest : ScenarioTestBase() {

    private val outriderAbilityId by lazy {
        cardRegistry.requireCard("Verdant Outrider").activatedAbilities[0].id
    }

    init {
        context("Tattered Ratter — pumps whichever Rat you control got blocked") {
            test("a blocked Rat gets +2/+0, not the Ratter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tattered Ratter", summoningSickness = false)
                    .withCardOnBattlefield(1, "Mind Drill Assailant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Mind Drill Assailant is a 2/5 Rat Warlock; the Ratter itself is a Human Peasant.
                val rat = game.findPermanent("Mind Drill Assailant")!!
                val ratter = game.findPermanent("Tattered Ratter")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Mind Drill Assailant" to 2, "Tattered Ratter" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf("Grizzly Bears" to listOf("Mind Drill Assailant"))
                ).error shouldBe null
                game.resolveStack()

                withClue("the blocked Rat gets +2/+0 (2/5 -> 4/5)") {
                    game.state.projectedState.getPower(rat) shouldBe 4
                    game.state.projectedState.getToughness(rat) shouldBe 5
                }
                withClue("the unblocked, non-Rat Ratter is untouched") {
                    game.state.projectedState.getPower(ratter) shouldBe 2
                }
            }

            test("a blocked non-Rat doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tattered Ratter", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val attackingBear = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Craw Wurm" to listOf("Grizzly Bears"))).error shouldBe null
                game.resolveStack()

                withClue("Bears aren't Rats — no pump") {
                    game.state.projectedState.getPower(attackingBear) shouldBe 2
                }
            }
        }
    }
}
