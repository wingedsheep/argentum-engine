package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Famished Foragers (MID #138) — {3}{R} 4/3.
 *
 *   When this creature enters, if an opponent lost life this turn, add {R}{R}{R}.
 *   {2}{R}, Discard a card: Draw a card.
 *
 * Proves the intervening "if" gates the triggered mana and the rummage ability's composite cost.
 */
class FamishedForagersScenarioTest : ScenarioTestBase() {

    init {
        context("Famished Foragers") {

            test("adds {R}{R}{R} when an opponent already lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Famished Foragers")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Famished Foragers").error shouldBe null
                game.resolveStack()

                game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 3
            }

            test("adds no mana when no opponent lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Famished Foragers")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Famished Foragers").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Famished Foragers") shouldBe true
                (game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red ?: 0) shouldBe 0
            }

            test("{2}{R}, discard a card: draw a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Famished Foragers", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val foragers = game.findPermanent("Famished Foragers")!!
                val abilityId = cardRegistry.getCard("Famished Foragers")!!.activatedAbilities.first().id
                val toDiscard = game.findCardsInHand(1, "Grizzly Bears").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = foragers,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
            }
        }
    }
}
