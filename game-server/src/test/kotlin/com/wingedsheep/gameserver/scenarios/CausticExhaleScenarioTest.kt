package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Caustic Exhale (TDM) — {B} Instant.
 *
 * "As an additional cost to cast this spell, behold a Dragon or pay {1}.
 *  Target creature gets -3/-3 until end of turn."
 *
 * Exercises the `AdditionalCost.BeholdOrPay` cost (Dragon filter) plus the {1}-mana
 * alternative, with the -3/-3 spell effect on a targeted creature.
 */
class CausticExhaleScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Caustic Exhale behold-or-pay cost") {

            test("behold a Dragon in hand to cast for just {B}, target gets -3/-3") {
                // Player 1 has Caustic Exhale + a Dragon to behold, and only one Swamp ({B}).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Caustic Exhale")
                    .withCardInHand(1, "Kilnmouth Dragon")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Hill Giant")!!

                // Behold the Dragon (reveal from hand) — cost is just {B}, with target on Hill Giant.
                val hand = game.state.getHand(game.player1Id)
                val cardId = hand.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Caustic Exhale"
                }
                val dragonId = hand.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Kilnmouth Dragon"
                }
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(target)),
                        additionalCostPayment = AdditionalCostPayment(beheldCards = listOf(dragonId))
                    )
                )
                withClue("Cast via behold should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Hill Giant (3/3) gets -3/-3 → 0/0 and dies") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }

            test("pay {1} instead of beholding") {
                // No Dragon — pay the {1} alternative. Needs {B} + {1} = 2 mana.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Caustic Exhale")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Caustic Exhale", target)
                withClue("Cast paying {1} should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears (2/2) gets -3/-3 → -1/-1 and dies") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
