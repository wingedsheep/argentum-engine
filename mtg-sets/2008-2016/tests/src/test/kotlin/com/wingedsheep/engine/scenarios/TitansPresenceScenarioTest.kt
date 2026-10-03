package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Titan's Presence (BFZ #14) — {3} instant. "As an additional cost to cast this spell, reveal a
 * colorless creature card from your hand. Exile target creature if its power is less than or equal
 * to the revealed card's power."
 */
class TitansPresenceScenarioTest : ScenarioTestBase() {
    init {
        fun board(revealable: List<String>, opponentCreature: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Titan's Presence")
            .apply { revealable.forEach { withCardInHand(1, it) } }
            .withLandsOnBattlefield(1, "Plains", 3)
            .withCardOnBattlefield(2, opponentCreature)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.handCard(name: String): EntityId =
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

        fun TestGame.castRevealing(revealed: String, target: EntityId) = execute(
            CastSpell(
                playerId = player1Id,
                cardId = handCard("Titan's Presence"),
                targets = listOf(ChosenTarget.Permanent(target)),
                additionalCostPayment = AdditionalCostPayment(revealedCards = listOf(handCard(revealed))),
            )
        )

        test("exiles a creature whose power is at most the revealed card's; the revealed card stays in hand") {
            val game = board(listOf("Juggernaut"), "Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!

            game.castRevealing("Juggernaut", giant).error shouldBe null
            game.resolveStack()

            game.findPermanent("Hill Giant") shouldBe null
            game.state.getExile(game.player2Id) shouldContain giant
            game.state.getHand(game.player1Id) shouldContain game.handCard("Juggernaut")
        }

        test("equal power is enough") {
            val game = board(listOf("Ornithopter"), "Ornithopter")
            val opposing = game.findPermanent("Ornithopter")!!

            game.castRevealing("Ornithopter", opposing).error shouldBe null
            game.resolveStack()

            withClue("0 power ≤ 0 revealed") {
                game.state.getExile(game.player2Id) shouldContain opposing
            }
        }

        test("a creature with more power than the revealed card stays") {
            val game = board(listOf("Juggernaut"), "Craw Wurm")
            val wurm = game.findPermanent("Craw Wurm")!!

            game.castRevealing("Juggernaut", wurm).error shouldBe null
            game.resolveStack()

            withClue("6 power > 5 revealed: the spell resolves with no effect") {
                game.findPermanent("Craw Wurm") shouldNotBe null
            }
        }

        test("only a colorless creature card can be revealed") {
            val game = board(listOf("Grizzly Bears", "Juggernaut"), "Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!

            withClue("a green creature card can't pay the reveal") {
                (game.castRevealing("Grizzly Bears", giant).error != null) shouldBe true
            }
            val cast = game.getLegalActions(1)
                .first { (it.action as? CastSpell)?.cardId == game.handCard("Titan's Presence") }
            cast.additionalCostInfo!!.validRevealTargets shouldBe listOf(game.handCard("Juggernaut"))
        }
    }
}
