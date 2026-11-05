package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Infamous Cruelclaw (BLB #219) — {1}{B}{R} Legendary Creature — Weasel Mercenary 3/3, menace.
 *
 * "Whenever The Infamous Cruelclaw deals combat damage to a player, exile cards from the top of
 *  your library until you exile a nonland card. You may cast that card by discarding a card rather
 *  than paying its mana cost."
 *
 * Per the 2024-07-26 ruling the card is cast while the ability resolves — it can't be held for
 * later in the turn — and a declined card stays in exile.
 */
class TheInfamousCruelclawScenarioTest : ScenarioTestBase() {

    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Infamous Cruelclaw", summoningSickness = false)
            .withCardInHand(1, "Grizzly Bears")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.exiled(name: String): List<EntityId> =
            state.getExile(player1Id).filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

        fun TestGame.connect() {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("The Infamous Cruelclaw" to 2))
            passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            declareNoBlockers()
            passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            resolveStack() // stops at the cast offer
        }

        test("the exiled nonland card is cast during resolution by discarding a card") {
            val game = board()
            game.connect()

            withClue("Cruelclaw offers the exiled Hill Giant") { game.hasPendingDecision() shouldBe true }
            game.selectCards(game.exiled("Hill Giant"))
            if (game.hasPendingDecision()) game.selectCards(game.findCardsInHand(1, "Grizzly Bears"))
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            withClue("the discard paid for it — no lands on the board") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            game.isInExile(1, "Mountain") shouldBe true
            game.getLifeTotal(2) shouldBe 17
        }

        test("a declined card stays in exile and can't be cast later in the turn") {
            val game = board()
            game.connect()

            game.skipSelection()
            game.resolveStack()

            val giant = game.exiled("Hill Giant").single()
            game.isInHand(1, "Grizzly Bears") shouldBe true
            withClue("no lingering cast-from-exile permission") {
                game.state.mayPlayPermissions.any { giant in it.cardIds } shouldBe false
            }
        }
    }
}
