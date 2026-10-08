package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for `Effects.TapEachTarget()` — "tap up to N target creatures".
 *
 * This composition (ForEachTarget over Effects.Tap) replaced the old
 * `TapTargetCreaturesEffect`, whose `maxTargets` field duplicated the count owned by the
 * spell's TargetCreature and was abused as a magic `20` to mean "any number". These tests
 * pin both wirings:
 *   - fixed-count targeting (Tidal Surge: "up to three"),
 *   - X-driven targeting (Icy Blast: "tap X target creatures", exactly X via `targets(…, exactly = X)`),
 *     which is the regression guard for dropping the magic `count = 20`.
 */
class TapEachTargetScenarioTest : ScenarioTestBase() {

    private fun TestGame.handCardId(playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getHand(playerId).first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }
    }

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.has<TappedComponent>() == true

    init {
        context("Effects.TapEachTarget — fixed count (Tidal Surge)") {
            test("taps every targeted creature, leaves untargeted ones untapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tidal Surge")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findAllPermanents("Grizzly Bears")
                bears.size shouldBe 3

                // Target two of the three bears.
                val targeted = bears.take(2)
                val untargeted = bears.drop(2)

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.handCardId(1, "Tidal Surge"),
                        targeted.map { ChosenTarget.Permanent(it) }
                    )
                )
                withClue("Tidal Surge should cast: ${cast.error}") { cast.error shouldBe null }

                game.resolveStack()

                withClue("both targeted creatures should be tapped") {
                    targeted.all { game.isTapped(it) } shouldBe true
                }
                withClue("the untargeted creature should remain untapped") {
                    untargeted.none { game.isTapped(it) } shouldBe true
                }
            }
        }

        context("Effects.TapEachTarget — X-clamped count (Icy Blast)") {
            test("taps exactly the X creatures chosen as targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Icy Blast")
                    .withLandsOnBattlefield(1, "Island", 3) // {2}{U} at X=2
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findAllPermanents("Grizzly Bears")
                val targeted = bears.take(2)
                val untargeted = bears.drop(2)

                // X = 2: "X target creatures" requires exactly two targets.
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.handCardId(1, "Icy Blast"),
                        targeted.map { ChosenTarget.Permanent(it) },
                        xValue = 2
                    )
                )
                withClue("Icy Blast (X=2) should cast with two targets: ${cast.error}") {
                    cast.error shouldBe null
                }

                game.resolveStack()

                withClue("both X-chosen creatures should be tapped") {
                    targeted.all { game.isTapped(it) } shouldBe true
                }
                withClue("the creature outside X should remain untapped") {
                    untargeted.none { game.isTapped(it) } shouldBe true
                }
            }
        }
    }
}
