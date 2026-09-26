package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.StruggleForSanity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Struggle for Sanity (CHK #145) — "Target opponent reveals their hand. That player exiles a card
 * from it, then you exile a card from it. Repeat this process until all cards in that hand have
 * been exiled. That player returns the cards they exiled this way to their hand and puts the rest
 * into their graveyard."
 *
 * The opponent's picks from *every* pass come back to hand and the caster's go to the graveyard —
 * the loop has to remember both players' picks across passes, not just the last one.
 */
class StruggleForSanityScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + StruggleForSanity)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Empty [player]'s hand, then fill it with exactly [names]. */
    fun GameTestDriver.setHand(player: EntityId, vararg names: String): List<EntityId> {
        getHand(player).forEach { moveToGraveyard(it) }
        return names.map { putCardInHand(player, it) }
    }

    /**
     * Answer every card choice the spell raises: each chooser takes the first card of their own
     * preference list still on offer. Returns who was asked, in order.
     */
    fun GameTestDriver.driveLoop(
        opponent: EntityId,
        opponentPicks: List<EntityId>,
        myPicks: List<EntityId>
    ): List<EntityId> {
        val askedInOrder = mutableListOf<EntityId>()
        var guard = 0
        while (guard++ < 20) {
            val decision = state.pendingDecision as? SelectCardsDecision ?: break
            askedInOrder += decision.playerId
            val preference = if (decision.playerId == opponent) opponentPicks else myPicks
            val pick = preference.first { it in decision.options }
            submitCardSelection(decision.playerId, listOf(pick))
        }
        return askedInOrder
    }

    test("the opponent's picks from every pass return to hand; the caster's go to the graveyard") {
        val d = driver()
        val me = d.player1
        val opponent = d.player2
        val spell = d.putCardInHand(me, "Struggle for Sanity")
        d.giveMana(me, Color.BLACK, 4)
        val (bears, bolt, growth, courser, forest) = d.setHand(
            opponent, "Grizzly Bears", "Lightning Bolt", "Giant Growth", "Centaur Courser", "Forest"
        )
        val graveyardBefore = d.getGraveyardCardNames(opponent)

        d.castSpell(me, spell, targets = listOf(opponent))
        d.bothPass()

        // Pass 1: they exile Bears, you exile Bolt. Pass 2: they exile Giant Growth, you exile
        // Courser. Pass 3: only Forest is left — they exile it and you have nothing to take.
        val asked = d.driveLoop(
            opponent,
            opponentPicks = listOf(bears, growth, forest),
            myPicks = listOf(bolt, courser)
        )

        withClue("each pass asks the opponent first, then the caster") {
            asked.take(4) shouldBe listOf(opponent, me, opponent, me)
        }
        withClue("every card the opponent exiled came back to their hand") {
            d.getHand(opponent).map { d.getCardName(it) } shouldContainExactlyInAnyOrder
                listOf("Grizzly Bears", "Giant Growth", "Forest")
        }
        withClue("every card the caster exiled went to the opponent's graveyard") {
            (d.getGraveyardCardNames(opponent) - graveyardBefore.toSet()) shouldContainExactlyInAnyOrder
                listOf("Lightning Bolt", "Centaur Courser")
        }
        withClue("nothing is left in exile") {
            d.getExileCardNames(opponent).shouldBeEmpty()
        }
        d.state.pendingDecision shouldBe null
    }

    test("an opponent with a one-card hand keeps it") {
        val d = driver()
        val me = d.player1
        val opponent = d.player2
        val spell = d.putCardInHand(me, "Struggle for Sanity")
        d.giveMana(me, Color.BLACK, 4)
        val (bolt) = d.setHand(opponent, "Lightning Bolt")

        d.castSpell(me, spell, targets = listOf(opponent))
        d.bothPass()
        d.driveLoop(opponent, opponentPicks = listOf(bolt), myPicks = emptyList())

        withClue("the opponent exiled their only card, so it came straight back") {
            d.getHand(opponent).map { d.getCardName(it) } shouldBe listOf("Lightning Bolt")
            d.getExileCardNames(opponent).shouldBeEmpty()
        }
        d.state.pendingDecision shouldBe null
    }

    test("against an empty hand the spell simply resolves") {
        val d = driver()
        val me = d.player1
        val opponent = d.player2
        val spell = d.putCardInHand(me, "Struggle for Sanity")
        d.giveMana(me, Color.BLACK, 4)
        d.setHand(opponent)

        d.castSpell(me, spell, targets = listOf(opponent))
        d.bothPass()

        d.state.pendingDecision shouldBe null
        d.getHand(opponent).shouldBeEmpty()
        d.getGraveyardCardNames(me) shouldBe listOf("Struggle for Sanity")
    }
})
