package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cn2.cards.SelvalasStampede
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Selvala's Stampede (CN2 #71) — {4}{G}{G} Sorcery.
 *
 *   "Council's dilemma — Starting with you, each player votes for wild or free. Reveal cards from
 *    the top of your library until you reveal a creature card for each wild vote. Put those
 *    creature cards onto the battlefield, then shuffle the rest into your library. You may put a
 *    permanent card from your hand onto the battlefield for each free vote."
 *
 * Three players, so both tallies can be more than one and differ: every vote adds to its effect.
 */
class SelvalasStampedeScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val p1: EntityId, val p2: EntityId, val p3: EntityId)

    fun table(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SelvalasStampede)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(6) { d.putLandOnBattlefield(p1, "Forest") }
        return Table(d, p1, p2, p3)
    }

    /** Cast the Stampede and cast [votes] in turn order (`"wild"` / `"free"`); returns the voters. */
    fun Table.castAndVote(votes: List<String>): List<EntityId> {
        val stampede = d.putCardInHand(p1, "Selvala's Stampede")
        d.castSpell(p1, stampede).error shouldBe null
        var guard = 0
        while (guard++ < 10 && d.pendingDecision == null && d.stackSize > 0) d.bothPass()
        return votes.map { vote ->
            val ballot = d.pendingDecision as ChooseOptionDecision
            ballot.options shouldBe listOf("Wild", "Free")
            d.submitDecision(ballot.playerId, OptionChosenResponse(ballot.id, if (vote == "wild") 0 else 1)).error shouldBe null
            ballot.playerId
        }
    }

    fun Table.battlefield() = d.state.getZone(ZoneKey(p1, Zone.BATTLEFIELD))

    test("each wild vote puts one revealed creature onto the battlefield; the rest are shuffled back") {
        val t = table()
        // Top to bottom: Grizzly Bears, Mountain, Centaur Courser, Hill Giant.
        val giant = t.d.putCardOnTopOfLibrary(t.p1, "Hill Giant")
        val courser = t.d.putCardOnTopOfLibrary(t.p1, "Centaur Courser")
        val mountain = t.d.putCardOnTopOfLibrary(t.p1, "Mountain")
        val bears = t.d.putCardOnTopOfLibrary(t.p1, "Grizzly Bears")

        val voters = t.castAndVote(listOf("wild", "wild", "wild"))
        withClue("starting with the caster, then in turn order") { voters shouldBe listOf(t.p1, t.p2, t.p3) }

        withClue("no free votes — nothing is asked from hand") { t.d.pendingDecision shouldBe null }
        t.battlefield() shouldContain bears
        t.battlefield() shouldContain courser
        t.battlefield() shouldContain giant
        withClue("the revealed noncreature card went back into the library") {
            t.d.state.getZone(ZoneKey(t.p1, Zone.LIBRARY)) shouldContain mountain
        }
    }

    test("each free vote lets you put one permanent card from your hand onto the battlefield") {
        val t = table()
        val bears = t.d.putCardOnTopOfLibrary(t.p1, "Grizzly Bears")
        val giant = t.d.putCardInHand(t.p1, "Hill Giant")
        val courser = t.d.putCardInHand(t.p1, "Centaur Courser")
        val forest = t.d.putCardInHand(t.p1, "Forest")
        val bolt = t.d.putCardInHand(t.p1, "Lightning Bolt")

        t.castAndVote(listOf("free", "wild", "free"))

        withClue("one wild vote: the top creature enters") { t.battlefield() shouldContain bears }
        val choice = t.d.pendingDecision as SelectCardsDecision
        withClue("only permanent cards in hand are offered, up to one per free vote") {
            choice.options shouldContainAll listOf(giant, courser, forest)
            choice.options shouldNotContain bolt
            choice.minSelections shouldBe 0
            choice.maxSelections shouldBe 2
        }
        t.d.submitCardSelection(t.p1, listOf(giant, forest)).error shouldBe null

        t.battlefield() shouldContain giant
        t.battlefield() shouldContain forest
        t.battlefield() shouldNotContain courser
        t.d.getHand(t.p1) shouldContain bolt
    }

    test("no wild votes puts no creature onto the battlefield, and the free placements may be declined") {
        val t = table()
        val bears = t.d.putCardOnTopOfLibrary(t.p1, "Grizzly Bears")
        val before = t.battlefield().size
        t.castAndVote(listOf("free", "free", "free"))
        // "You may": decline every free-vote placement.
        t.d.submitCardSelection(t.p1, emptyList()).error shouldBe null
        t.battlefield() shouldNotContain bears
        t.battlefield().size shouldBe before
        t.d.state.getZone(ZoneKey(t.p1, Zone.LIBRARY)) shouldContain bears
    }
})
