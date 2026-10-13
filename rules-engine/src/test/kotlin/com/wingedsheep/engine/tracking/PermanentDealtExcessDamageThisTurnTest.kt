package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for `TurnTracker.PERMANENT_DEALT_EXCESS_DAMAGE` — "if a creature or planeswalker
 * an opponent controlled was dealt excess damage this turn" (Rith, Liberated Primeval).
 *
 * CR 120.4a: a creature's excess is the damage past lethal, counting damage already marked on it
 * and collapsing lethal to 1 for a deathtouch source; a planeswalker's is the damage past its
 * loyalty. Wither changes only the form of the damage (CR 702.80a), so its excess is the same.
 * The record is keyed on the recipient's controller as the damage was dealt and lasts the turn.
 */
class PermanentDealtExcessDamageThisTurnTest : FunSpec({

    val fourFour = card("Excess Probe Wall") {
        manaCost = "{3}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }

    val deathtouchOgre = card("Excess Probe Deathtoucher") {
        manaCost = "{2}"
        typeLine = "Creature — Ogre"
        power = 2
        toughness = 2
        keywords(Keyword.DEATHTOUCH)
    }

    val witherer = card("Excess Probe Witherer") {
        manaCost = "{3}"
        typeLine = "Creature — Elemental"
        power = 3
        toughness = 3
        keywords(Keyword.WITHER)
    }

    val walker = card("Excess Probe Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(fourFour, deathtouchOgre, witherer, walker))
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val evaluator = PredicateEvaluator(cardRegistry = null)

    /** Rith's condition, read from player 1's seat. */
    fun GameTestDriver.opponentRecorded(): Boolean =
        evaluator.conditions.evaluate(
            state,
            Conditions.OpponentPermanentWasDealtExcessDamageThisTurn,
            EffectContext(sourceId = null, controllerId = player1)
        )

    fun GameTestDriver.recorded(player: EntityId) = player in state.playersWithPermanentDealtExcessDamageThisTurn

    fun GameTestDriver.bolt(target: EntityId) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, targets = listOf(target)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.walkerWithLoyalty(player: EntityId, loyalty: Int): EntityId {
        val pw = putPermanentOnBattlefield(player, "Excess Probe Walker")
        replaceState(state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) })
        return pw
    }

    /** Player 1 attacks [defender] with [attacker]; player 2 blocks with [blocker] if given. */
    fun GameTestDriver.combat(attacker: EntityId, defender: EntityId, blocker: EntityId? = null) {
        removeSummoningSickness(attacker)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, mapOf(attacker to defender)).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        if (blocker != null) declareBlockers(player2, mapOf(blocker to listOf(attacker))).error shouldBe null
        else declareNoBlockers(player2)
        passPriorityUntil(Step.END_COMBAT)
    }

    test("noncombat damage past lethal is recorded against the creature's controller") {
        val d = driver()
        d.bolt(d.putCreatureOnBattlefield(d.player2, "Savannah Lions"))
        d.recorded(d.player2) shouldBe true
        d.recorded(d.player1) shouldBe false
        withClue("the creature died to that damage — turn history, not a board scan") {
            d.opponentRecorded() shouldBe true
        }
    }

    test("exactly lethal damage is not excess") {
        val d = driver()
        d.bolt(d.putCreatureOnBattlefield(d.player2, "Centaur Courser"))
        d.opponentRecorded() shouldBe false
    }

    test("damage already marked counts toward lethal (Scryfall ruling, 2022-09-09)") {
        val d = driver()
        val wall = d.putCreatureOnBattlefield(d.player2, "Excess Probe Wall")
        d.bolt(wall)
        withClue("3 damage to a 0/4 is short of lethal") { d.opponentRecorded() shouldBe false }
        d.bolt(wall)
        withClue("3 more with 3 already marked: lethal is 1, so 2 is excess") { d.opponentRecorded() shouldBe true }
    }

    test("your own creature dealt excess damage records you, not an opponent") {
        val d = driver()
        d.bolt(d.putCreatureOnBattlefield(d.player1, "Savannah Lions"))
        d.recorded(d.player1) shouldBe true
        d.opponentRecorded() shouldBe false
    }

    test("noncombat damage past a planeswalker's loyalty is excess; up to it is not") {
        val d = driver()
        d.bolt(d.walkerWithLoyalty(d.player2, 3))
        withClue("3 damage to loyalty 3") { d.opponentRecorded() shouldBe false }
        d.bolt(d.walkerWithLoyalty(d.player2, 2))
        withClue("3 damage to loyalty 2") { d.opponentRecorded() shouldBe true }
    }

    test("combat damage past a blocker's toughness is recorded against the blocker's controller") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        d.combat(courser, d.player2, blocker = lions)
        d.recorded(d.player2) shouldBe true
        withClue("the 1/1 Lions dealt 1 to the 3/3 attacker — not lethal, no excess for its controller") {
            d.recorded(d.player1) shouldBe false
        }
    }

    test("a deathtouch source makes any damage past 1 excess, whatever the toughness") {
        val d = driver()
        val ogre = d.putCreatureOnBattlefield(d.player1, "Excess Probe Deathtoucher")
        val wall = d.putCreatureOnBattlefield(d.player2, "Excess Probe Wall")
        d.combat(ogre, d.player2, blocker = wall)
        d.opponentRecorded() shouldBe true
    }

    test("wither damage past lethal is excess even though it is dealt as -1/-1 counters") {
        val d = driver()
        val withering = d.putCreatureOnBattlefield(d.player1, "Excess Probe Witherer")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        d.combat(withering, d.player2, blocker = lions)
        d.opponentRecorded() shouldBe true
    }

    test("combat damage past an attacked planeswalker's loyalty is excess") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.combat(courser, d.walkerWithLoyalty(d.player2, 2))
        d.opponentRecorded() shouldBe true
    }

    test("combat damage to a player is never excess") {
        val d = driver()
        d.combat(d.putCreatureOnBattlefield(d.player1, "Centaur Courser"), d.player2)
        d.recorded(d.player2) shouldBe false
    }

    test("the record clears at the turn boundary") {
        val d = driver()
        d.bolt(d.putCreatureOnBattlefield(d.player2, "Savannah Lions"))
        d.opponentRecorded() shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.turnNumber shouldBe 2
        d.opponentRecorded() shouldBe false
    }
})
