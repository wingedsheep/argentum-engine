package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Burn from Within (SOI #148) — {X}{R} Sorcery.
 *
 *   Burn from Within deals X damage to any target. If a creature is dealt damage this way, it loses
 *   indestructible until end of turn. If that creature would die this turn, exile it instead.
 *
 * Both riders are gated on the damage actually dealt and on the target being a creature, and they
 * land during resolution — before state-based actions see the lethal damage — so an indestructible
 * creature dies, and the exile-instead mark lasts for the rest of the turn.
 */
class BurnFromWithinScenarioTest : ScenarioTestBase() {

    init {
        context("Burn from Within") {

            test("an indestructible creature dealt lethal X loses indestructible and is exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Burn from Within")
                    .withCardOnBattlefield(2, "Darksteel Myr")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myr = game.findPermanent("Darksteel Myr")!!
                game.castXSpell(1, "Burn from Within", 1, myr).error shouldBe null
                game.resolveStack()

                withClue("1 damage to the 0/1 indestructible Myr is lethal once it loses indestructible") {
                    game.isOnBattlefield("Darksteel Myr") shouldBe false
                    game.isInGraveyard(2, "Darksteel Myr") shouldBe false
                    game.isInExile(2, "Darksteel Myr") shouldBe true
                }
            }

            test("targeting a player deals X damage and touches nothing else") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Burn from Within")
                    .withCardOnBattlefield(2, "Darksteel Myr")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Burn from Within"
                }
                val cast = game.execute(
                    CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Player(game.player2Id)), 3)
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.isInGraveyard(1, "Burn from Within") shouldBe true
                withClue("the opponent's indestructible creature wasn't dealt damage, so it keeps indestructible") {
                    val myr = game.findPermanent("Darksteel Myr")!!
                    game.isOnBattlefield("Darksteel Myr") shouldBe true
                    game.state.projectedState.hasKeyword(myr, Keyword.INDESTRUCTIBLE) shouldBe true
                }
            }

            test("a creature dealt non-lethal damage is exiled when it dies later that turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Burn from Within")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castXSpell(1, "Burn from Within", 1, bears).error shouldBe null
                game.resolveStack()

                withClue("1 damage doesn't kill the 2/2 Bears") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("the Bears die to Lightning Bolt the same turn, so they're exiled instead") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
