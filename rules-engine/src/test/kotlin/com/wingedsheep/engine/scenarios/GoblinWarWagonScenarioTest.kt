package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Goblin War Wagon (MRD #179).
 *
 * {4} Artifact Creature — Juggernaut 3/3
 * "This creature doesn't untap during your untap step.
 *  At the beginning of your upkeep, you may pay {2}. If you do, untap this creature."
 *
 * The interesting bit is the *self-scoped* untap restriction: every other user of
 * `AbilityFlag.DOESNT_UNTAP` in the catalog is an Aura granting it to the enchanted creature,
 * so this is the first card applying it to itself via `GroupFilter.source()`. These tests prove
 * the restriction actually survives projection and is read by the untap step, and that the
 * upkeep may-pay is the only way back.
 */
class GoblinWarWagonScenarioTest : ScenarioTestBase() {

    init {
        fun isTapped(game: TestGame, id: EntityId): Boolean =
            game.state.getEntity(id)?.get<TappedComponent>() != null

        context("Goblin War Wagon") {

            test("stays tapped through its controller's untap step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Goblin War Wagon", tapped = true)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val wagon = game.findPermanent("Goblin War Wagon")!!
                isTapped(game, wagon) shouldBe true

                // Cross into player 1's turn — the untap step runs on the way.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("DOESNT_UNTAP kept the Wagon tapped through the untap step") {
                    isTapped(game, wagon) shouldBe true
                }
            }

            test("paying {2} at upkeep untaps it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Goblin War Wagon", tapped = true)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val wagon = game.findPermanent("Goblin War Wagon")!!
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // The upkeep trigger is on the stack; resolving it surfaces the optional payment.
                game.resolveStack()

                withClue("Upkeep trigger offers the optional {2} payment") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Paying {2} untaps the Wagon") {
                    isTapped(game, wagon) shouldBe false
                }
            }

            test("declining the payment leaves it tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Goblin War Wagon", tapped = true)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val wagon = game.findPermanent("Goblin War Wagon")!!
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Declining the may-pay leaves the Wagon tapped") {
                    isTapped(game, wagon) shouldBe true
                }
            }

            test("the restriction is scoped to the Wagon — other permanents untap normally") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Goblin War Wagon", tapped = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val wagon = game.findPermanent("Goblin War Wagon")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("GroupFilter.source() confines the flag to the Wagon itself") {
                    isTapped(game, wagon) shouldBe true
                    isTapped(game, bears) shouldBe false
                }
            }
        }
    }
}
