package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.AOrcishBowmasters
import com.wingedsheep.mtg.sets.definitions.ltr.cards.OrcishBowmasters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Orcish Bowmasters / A-Orcish Bowmasters and the underlying
 * [com.wingedsheep.sdk.dsl.Triggers.OpponentDrawsExceptFirstEachDrawStep] primitive.
 *
 * Rules proven (every clause has a paired assertion):
 *  - CR 504.1 — the first card a player draws in their own draw step (the turn-based draw) is
 *    EXEMPT; that normal for-turn draw does not fire the trigger.
 *  - "except the first one they draw in each of their draw steps" — the *second and later* cards an
 *    opponent draws during their draw step DO fire (one per card).
 *  - CR 121.2 — every other draw is an individual draw, so drawing N cards outside the draw step
 *    fires the trigger N times.
 *  - The trigger watches opponents only — the controller's own draws never fire it.
 *  - The printed card's combined "enters / opponent draws" ability fires on enter; the Alchemy
 *    rebalance (A-) drops the enter trigger.
 *  - The effect is "deal 1 damage to any target. Then amass Orcs 1." per firing.
 *  - Batch boundary: when one resolution emits several CardsDrawnEvents for the same player
 *    while the exempt slot is open, the exemption lands on the first card, not on none of them.
 *  - The plain [com.wingedsheep.sdk.dsl.Triggers.OpponentDraws] variant has no exemption — the
 *    for-turn draw fires it.
 */
class OrcishBowmastersTest : FunSpec({

    val projector = StateProjector()

    // {0} draw spells so the substrate is deterministic.
    val DrawTwo = card("Draw Two Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Draw two cards."
        spell { effect = Effects.DrawCards(2) }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T01" }
    }
    val DrawInstant = card("Draw Instant Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T02" }
    }
    // Two SEPARATE draw effects in one resolution → two CardsDrawnEvents in one trigger-detection
    // batch (unlike DrawTwo's single count-2 event). Exercises the batch-boundary exemption math.
    val DrawTwiceSeparately = card("Draw Twice Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card. Draw a card."
        spell { effect = Effects.Composite(Effects.DrawCards(1), Effects.DrawCards(1)) }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T03" }
    }
    // Plain OpponentDraws (no draw-step exemption) — the for-turn draw fires it too.
    val DrawWatcher = card("Draw Watcher Test") {
        manaCost = "{B}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        oracleText = "Whenever an opponent draws a card, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.OpponentDraws
            effect = Effects.GainLife(1)
        }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T04" }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                OrcishBowmasters, AOrcishBowmasters, DrawTwo, DrawInstant, DrawTwiceSeparately, DrawWatcher
            )
        )
        return driver
    }

    fun GameTestDriver.armiesControlledBy(player: EntityId): List<EntityId> {
        val projected = projector.project(state)
        return projected.getBattlefieldControlledBy(player)
            .filter { projected.isCreature(it) && projected.hasSubtype(it, "Army") }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /**
     * Resolve everything currently on the stack, answering each Bowmasters target prompt by
     * pointing the 1 damage at [target]. Returns the number of target prompts answered (= the
     * number of Bowmasters firings).
     */
    fun GameTestDriver.resolveStackPinging(controller: EntityId, target: EntityId): Int {
        var pings = 0
        while (true) {
            val decision = pendingDecision
            if (decision is ChooseTargetsDecision) {
                submitTargetSelection(controller, listOf(target))
                pings++
                continue
            }
            if (getTopOfStack() != null) {
                bothPass()
                continue
            }
            break
        }
        return pings
    }

    test("the opponent's normal for-turn draw is exempt (CR 504.1) — no trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(p1, "Orcish Bowmasters")
        driver.setLifeTotal(p2, 20)

        // Advance into p2's turn. p2's draw step performs the turn-based for-turn draw — the
        // exempt first card. Bowmasters must NOT trigger.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        (driver.pendingDecision == null) shouldBe true
        driver.getLifeTotal(p2) shouldBe 20
        driver.armiesControlledBy(p1).size shouldBe 0
    }

    test("the opponent's SECOND draw-step draw is not exempt — fires once (1 damage + amass)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40, "Draw Instant Test" to 10))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(p1, "Orcish Bowmasters")
        driver.setLifeTotal(p2, 20)

        // Into p2's turn, stop in their draw step *after* the exempt for-turn draw, while p2 holds
        // priority. p2 draws a second card this draw step via an instant → that one fires Bowmasters.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.DRAW)

        val instant = driver.putCardInHand(p2, "Draw Instant Test")
        driver.castSpell(p2, instant)
        val pings = driver.resolveStackPinging(p1, p2)

        pings shouldBe 1
        driver.getLifeTotal(p2) shouldBe 19
        val army = driver.armiesControlledBy(p1).single()
        driver.plusOneCounters(army) shouldBe 1
    }

    test("drawing N cards outside the draw step fires N times (CR 121.2)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40, "Draw Two Test" to 10))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(p1, "Orcish Bowmasters")
        driver.setLifeTotal(p2, 20)

        // p2's main phase (after the exempt for-turn draw): a Draw-2 spell draws two non-exempt
        // cards → two separate firings → 2 damage and amass twice onto the same Army (1/1 → 2/2).
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val draw2 = driver.putCardInHand(p2, "Draw Two Test")
        driver.castSpell(p2, draw2)
        val pings = driver.resolveStackPinging(p1, p2)

        pings shouldBe 2
        driver.getLifeTotal(p2) shouldBe 18
        val army = driver.armiesControlledBy(p1).single()
        driver.plusOneCounters(army) shouldBe 2
    }

    test("two separate draw effects in one resolution: the draw-step exemption still lands on the first card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40, "Draw Twice Test" to 10))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        // Bowmasters for p2 before p1's turn-1 draw step. The first player's turn-based draw is
        // skipped on turn 1, so the exempt "first card drawn this draw step" slot is still open
        // when the spell below resolves.
        driver.putCreatureOnBattlefield(p2, "Orcish Bowmasters")
        driver.setLifeTotal(p1, 20)
        driver.passPriorityUntil(Step.DRAW)

        // One resolution, two separate DrawCards(1) effects → two CardsDrawnEvents detected
        // against the same post-resolution state. The first card is the exempt one; only the
        // second may fire.
        val spell = driver.putCardInHand(p1, "Draw Twice Test")
        driver.castSpell(p1, spell)
        val pings = driver.resolveStackPinging(p2, p1)

        pings shouldBe 1
        driver.getLifeTotal(p1) shouldBe 19
    }

    test("plain OpponentDraws fires on every opponent draw, including the for-turn draw") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(p1, "Draw Watcher Test")
        driver.setLifeTotal(p1, 20)

        // Into p2's turn: their turn-based draw is NOT exempt for the plain variant — gain 1 life.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.getLifeTotal(p1) shouldBe 21
    }

    test("an opponent drawing on YOUR turn fires (not their draw step)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40, "Draw Instant Test" to 10))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(p1, "Orcish Bowmasters")
        driver.setLifeTotal(p2, 20)

        // Still p1's turn. p1 passes priority so p2 can cast an instant that draws — not in p2's
        // draw step → fires.
        driver.passPriority(p1)
        val instant = driver.putCardInHand(p2, "Draw Instant Test")
        driver.castSpell(p2, instant)
        val pings = driver.resolveStackPinging(p1, p2)

        pings shouldBe 1
        driver.getLifeTotal(p2) shouldBe 19
    }

    test("the controller's own draws never fire the trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40, "Draw Two Test" to 10))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(p1, "Orcish Bowmasters")
        driver.setLifeTotal(p1, 20)
        driver.setLifeTotal(p2, 20)

        // p1 (the controller) draws two cards — Player.EachOpponent binding means no firing.
        val draw2 = driver.putCardInHand(p1, "Draw Two Test")
        driver.castSpell(p1, draw2)
        driver.bothPass()

        (driver.pendingDecision == null) shouldBe true
        driver.getLifeTotal(p1) shouldBe 20
        driver.getLifeTotal(p2) shouldBe 20
        driver.armiesControlledBy(p1).size shouldBe 0
    }

    test("the printed card's enter trigger deals 1 and amasses") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.setLifeTotal(p2, 20)

        driver.giveMana(p1, Color.BLACK, 2)
        val bowmasters = driver.putCardInHand(p1, "Orcish Bowmasters")
        driver.castSpell(p1, bowmasters)
        driver.bothPass() // resolve the creature spell; ETB trigger goes on the stack and asks for a target
        val pings = driver.resolveStackPinging(p1, p2)

        pings shouldBe 1
        driver.getLifeTotal(p2) shouldBe 19
        val army = driver.armiesControlledBy(p1).single()
        driver.plusOneCounters(army) shouldBe 1
    }

    test("A-Orcish Bowmasters (rebalanced) has no enter trigger but still fires on opponent draws") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40, "Draw Instant Test" to 10))
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.setLifeTotal(p2, 20)

        driver.giveMana(p1, Color.BLACK, 2)
        val bowmasters = driver.putCardInHand(p1, "A-Orcish Bowmasters")
        driver.castSpell(p1, bowmasters)
        driver.bothPass() // creature resolves — NO enter trigger on the rebalanced card

        (driver.pendingDecision == null) shouldBe true
        driver.getLifeTotal(p2) shouldBe 20
        driver.armiesControlledBy(p1).size shouldBe 0

        // But it still pings on an opponent's non-exempt draw. p1 passes so p2 can cast.
        driver.passPriority(p1)
        val instant = driver.putCardInHand(p2, "Draw Instant Test")
        driver.castSpell(p2, instant)
        val pings = driver.resolveStackPinging(p1, p2)

        pings shouldBe 1
        driver.getLifeTotal(p2) shouldBe 19
    }
})
