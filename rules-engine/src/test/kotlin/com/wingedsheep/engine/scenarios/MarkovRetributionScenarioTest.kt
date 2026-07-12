package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Markov Retribution (VOW #171) — {2}{R} Sorcery.
 *
 *   Choose one or both —
 *   • Creatures you control get +1/+0 until end of turn.
 *   • Target Vampire you control deals damage equal to its power to another target creature.
 *
 * Exercises all three modes: the team pump alone, the Vampire-damage bite alone (targeting a
 * Vampire you control at index 0 and another creature at index 1), and choosing both.
 */
class MarkovRetributionScenarioTest : ScenarioTestBase() {

    init {
        context("Markov Retribution — choose one or both") {

            test("mode 0: creatures you control get +1/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Markov Retribution")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getPower(giant) shouldBe 3

                game.castSpellWithMode(1, "Markov Retribution", modeIndex = 0).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears gets +1/+0") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
                withClue("Hill Giant gets +1/+0") {
                    game.state.projectedState.getPower(giant) shouldBe 4
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }

            test("mode 1: target Vampire you control deals damage equal to its power to another target creature") {
                // Markov Purifier is a 2/3 (power 2), so it deals 2 damage — enough to kill a
                // 2-toughness creature like Grizzly Bears.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Markov Retribution")
                    .withCardOnBattlefield(1, "Markov Purifier", summoningSickness = false) // 2/3 Vampire Cleric
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vampire = game.findPermanent("Markov Purifier")!!
                val victim = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(vampire) shouldBe 2

                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Markov Retribution"
                }

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(vampire), ChosenTarget.Permanent(victim)),
                        chosenModes = listOf(1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(vampire), ChosenTarget.Permanent(victim))
                        )
                    )
                )
                withClue("casting mode 1 with the Vampire and victim targeted should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears takes 2 damage (equal to the Vampire's power) and dies (2 toughness)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("mode 2: choosing both applies the pump and the Vampire's damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Markov Retribution")
                    .withCardOnBattlefield(1, "Markov Purifier", summoningSickness = false) // 2/3 Vampire Cleric
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vampire = game.findPermanent("Markov Purifier")!!
                val victim = game.findPermanent("Grizzly Bears")!!

                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Markov Retribution"
                }

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(vampire), ChosenTarget.Permanent(victim)),
                        chosenModes = listOf(2),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(vampire), ChosenTarget.Permanent(victim))
                        )
                    )
                )
                withClue("casting mode 2 (both) should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Markov Purifier gets +1/+0 from the pump, becoming a 3/3") {
                    game.state.projectedState.getPower(vampire) shouldBe 3
                    game.state.projectedState.getToughness(vampire) shouldBe 3
                }
                withClue("Grizzly Bears (2 toughness) dies to 2 damage from the Vampire's power") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
