package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.CaitCageBrawler
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cait, Cage Brawler — "Whenever Cait attacks, you and defending player each draw a card, then
 * discard a card. Put two +1/+1 counters on Cait if you discarded the card with the greatest mana
 * value among those cards or tied for greatest."
 *
 * Pins the comparison between the two discarded cards: a win and a tie both grow Cait, a loss
 * doesn't, and each player picks their own discard.
 */
class CaitCageBrawlerScenarioTest : FunSpec({

    fun plusCounters(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /**
     * Cait attacks; each player discards the named card from their hand. Returns Cait's id.
     */
    fun attackAndDiscard(yourDiscard: String, theirDiscard: String): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CaitCageBrawler)
        d.initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        val me = d.player1
        val opp = d.player2
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cait = d.putCreatureOnBattlefield(me, "Cait, Cage Brawler")
        d.removeSummoningSickness(cait)
        val myCard = if (yourDiscard == "Mountain") null else d.putCardInHand(me, yourDiscard)
        val theirCard = if (theirDiscard == "Mountain") null else d.putCardInHand(opp, theirDiscard)
        val myHandBefore = d.getHandSize(me)
        val oppHandBefore = d.getHandSize(opp)

        withClue("Cait has indestructible during its controller's turn") {
            d.state.projectedState.hasKeyword(cait, Keyword.INDESTRUCTIBLE) shouldBe true
        }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, mapOf(cait to opp)).error shouldBe null

        var guard = 0
        while (d.pendingDecision == null && d.stackSize > 0 && guard++ < 20) d.passPriority(d.state.priorityPlayerId!!)

        val first = d.pendingDecision
        withClue("you choose your discard first: $first") {
            (first is SelectCardsDecision) shouldBe true
            first!!.playerId shouldBe me
        }
        first as SelectCardsDecision
        withClue("you drew a card before discarding") { first.options.size shouldBe myHandBefore + 1 }
        val myPick = myCard ?: first.options.first()
        d.submitCardSelection(me, listOf(myPick)).error shouldBe null

        val second = d.pendingDecision
        withClue("the defending player chooses their own discard: $second") {
            (second is SelectCardsDecision) shouldBe true
            second!!.playerId shouldBe opp
        }
        second as SelectCardsDecision
        withClue("the defending player drew a card before discarding") {
            second.options.size shouldBe oppHandBefore + 1
        }
        val theirPick = theirCard ?: second.options.first()
        d.submitCardSelection(opp, listOf(theirPick)).error shouldBe null

        guard = 0
        while (d.stackSize > 0 && guard++ < 20) d.passPriority(d.state.priorityPlayerId!!)

        withClue("each discarded card is in its owner's graveyard") {
            (myPick in d.getGraveyard(me)) shouldBe true
            (theirPick in d.getGraveyard(opp)) shouldBe true
        }
        return d to cait
    }

    test("your discard has the greater mana value - Cait gets two +1/+1 counters") {
        val (d, cait) = attackAndDiscard(yourDiscard = "Grizzly Bears", theirDiscard = "Mountain")
        plusCounters(d, cait) shouldBe 2
    }

    test("tied for greatest mana value - Cait gets two +1/+1 counters") {
        val (d, cait) = attackAndDiscard(yourDiscard = "Grizzly Bears", theirDiscard = "Grizzly Bears")
        plusCounters(d, cait) shouldBe 2
    }

    test("defending player's discard has the greater mana value - no counters") {
        val (d, cait) = attackAndDiscard(yourDiscard = "Mountain", theirDiscard = "Grizzly Bears")
        plusCounters(d, cait) shouldBe 0
    }

    test("Cait doesn't have indestructible during an opponent's turn") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CaitCageBrawler)
        d.initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val cait = d.putCreatureOnBattlefield(d.player1, "Cait, Cage Brawler")
        d.state.projectedState.hasKeyword(cait, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
