package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sigil of Valor (ORI #239) — "Whenever equipped creature attacks alone, it gets +1/+1 until end of
 * turn for each other creature you control."
 *
 * Pins the parts the script can't show on its own: "other" is relative to the equipped creature
 * (the trigger's triggering entity), not the Sigil; the opponent's creatures don't count; the bonus
 * is locked in as the ability resolves (a creature dying afterwards doesn't shrink it); and
 * attacking alongside another creature doesn't trigger it.
 */
class SigilOfValorScenarioTest : ScenarioTestBase() {

    init {
        context("Sigil of Valor") {
            test("equipped creature attacking alone gets +1/+1 for each other creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Sigil of Valor", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                withClue("two other creatures you control (the opponent's Craw Wurm doesn't count): 2/2 → 4/4") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }

            test("the bonus is fixed once the ability resolves") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Sigil of Valor", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(bears) shouldBe 4

                game.castSpell(1, "Shock", lions).error shouldBe null
                game.resolveStack()

                withClue("Savannah Lions died after resolution") {
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                }
                withClue("the bonus stays +2/+2") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }

            test("attacking alongside another creature doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Sigil of Valor", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }
    }
}
