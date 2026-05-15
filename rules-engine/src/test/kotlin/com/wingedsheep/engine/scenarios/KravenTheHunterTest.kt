package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.om1.cards.KravenTheHunter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kraven the Hunter:
 *
 * `{1}{B}{G}` Legendary Creature — Human Warrior Villain 4/3
 * Trample
 * Whenever a creature an opponent controls with the greatest power among creatures
 * that player controls dies, draw a card and put a +1/+1 counter on Kraven the Hunter.
 *
 * Coverage:
 * - opponent's strict-greatest creature dies (Doom Blade) → fires
 * - opponent's tied-greatest creature dies (Toralf-style ruling) → fires
 * - opponent's sole creature dies → fires (singleton is its own maximum)
 * - non-greatest opponent creature dies → does not fire
 * - your own creature dies → does not fire (opponentControls scope)
 * - opponent's greatest creature dies in combat → fires (validates that the trigger
 *   flows through TriggerDetector rather than a stack-only handler)
 * - stolen creature dies under thief's control → fires (validates lastKnownController:
 *   the dying creature's last-known controller drives the "creatures that player
 *   controls" group, not its owner)
 */
class KravenTheHunterTest : FunSpec({

    val bigBeast = CardDefinition.creature(
        name = "Test Beast 6/6",
        manaCost = ManaCost.parse("{4}{G}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 6,
        toughness = 6
    )
    val midBeast = CardDefinition.creature(
        name = "Test Beast 4/4",
        manaCost = ManaCost.parse("{2}{G}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 4,
        toughness = 4
    )
    val smallBeast = CardDefinition.creature(
        name = "Test Beast 2/2",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KravenTheHunter))
        driver.registerCard(bigBeast)
        driver.registerCard(midBeast)
        driver.registerCard(smallBeast)
        return driver
    }

    fun kravenCounters(driver: GameTestDriver, kravenId: EntityId): Int =
        driver.state.getEntity(kravenId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE)
            ?: 0

    fun killWithDoomBlade(driver: GameTestDriver, caster: EntityId, victim: EntityId) {
        driver.giveMana(caster, Color.BLACK, 2)
        val doom = driver.putCardInHand(caster, "Doom Blade")
        driver.castSpellWithTargets(caster, doom, listOf(ChosenTarget.Permanent(victim)))
        // Resolve Doom Blade, then resolve Kraven's trigger if it was put on the stack.
        driver.bothPass()
        driver.bothPass()
    }

    test("strict-greatest opponent creature dies — Kraven draws and gets a +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))

        val ap = driver.activePlayer!!
        val op = driver.getOpponent(ap)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kraven = driver.putCreatureOnBattlefield(ap, "Kraven the Hunter")
        driver.putCreatureOnBattlefield(op, "Test Beast 4/4")  // strict greatest
        driver.putCreatureOnBattlefield(op, "Test Beast 2/2")  // survivor

        val handBefore = driver.getHandSize(ap)

        // Doom Blade the 4/4 → strictly greatest among OP's creatures.
        val target = driver.state.getBattlefield()
            .first { id ->
                driver.state.getEntity(id)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                    ?.name == "Test Beast 4/4"
            }
        killWithDoomBlade(driver, ap, target)

        kravenCounters(driver, kraven) shouldBe 1
        driver.getHandSize(ap) shouldBe handBefore + 1
    }

    test("tied-greatest opponent creature dies — Kraven still fires (ties qualify)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))

        val ap = driver.activePlayer!!
        val op = driver.getOpponent(ap)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kraven = driver.putCreatureOnBattlefield(ap, "Kraven the Hunter")
        val tiedA = driver.putCreatureOnBattlefield(op, "Test Beast 4/4")
        driver.putCreatureOnBattlefield(op, "Test Beast 4/4")  // tied for greatest

        val handBefore = driver.getHandSize(ap)

        killWithDoomBlade(driver, ap, tiedA)

        // Tied-inclusive interpretation: the dying creature's power (4) is >= every
        // surviving creature's power (4), so it qualifies as having the greatest power.
        kravenCounters(driver, kraven) shouldBe 1
        driver.getHandSize(ap) shouldBe handBefore + 1
    }

    test("opponent's sole creature dies — Kraven fires (singleton is its own maximum)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))

        val ap = driver.activePlayer!!
        val op = driver.getOpponent(ap)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kraven = driver.putCreatureOnBattlefield(ap, "Kraven the Hunter")
        val sole = driver.putCreatureOnBattlefield(op, "Test Beast 2/2")  // only creature

        val handBefore = driver.getHandSize(ap)

        killWithDoomBlade(driver, ap, sole)

        kravenCounters(driver, kraven) shouldBe 1
        driver.getHandSize(ap) shouldBe handBefore + 1
    }

    test("non-greatest opponent creature dies — Kraven does not fire") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))

        val ap = driver.activePlayer!!
        val op = driver.getOpponent(ap)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kraven = driver.putCreatureOnBattlefield(ap, "Kraven the Hunter")
        driver.putCreatureOnBattlefield(op, "Test Beast 6/6")  // greatest, stays alive
        val victim = driver.putCreatureOnBattlefield(op, "Test Beast 2/2")  // 2 < 6

        val handBefore = driver.getHandSize(ap)

        killWithDoomBlade(driver, ap, victim)

        kravenCounters(driver, kraven) shouldBe 0
        driver.getHandSize(ap) shouldBe handBefore
    }

    test("your own greatest creature dies — Kraven does not fire") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))

        val ap = driver.activePlayer!!
        val op = driver.getOpponent(ap)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kraven = driver.putCreatureOnBattlefield(ap, "Kraven the Hunter")
        val ownGreatest = driver.putCreatureOnBattlefield(ap, "Test Beast 6/6")
        driver.putCreatureOnBattlefield(op, "Test Beast 4/4")  // opp's, stays alive

        val handBefore = driver.getHandSize(ap)

        // Active player kills their own creature (not an opponent's). Kraven must not fire.
        killWithDoomBlade(driver, ap, ownGreatest)

        kravenCounters(driver, kraven) shouldBe 0
        driver.getHandSize(ap) shouldBe handBefore
    }

    test("opponent's greatest creature dies in combat — Kraven fires") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))

        val ap = driver.activePlayer!!
        val op = driver.getOpponent(ap)

        // AP controls Kraven (witness) + a 6/6 (attacker that kills the blocker cleanly).
        // OP controls a 4/4 — their strict greatest. AP attacks with the 6/6; OP blocks
        // with the 4/4. Combat damage kills the 4/4 (6 damage to 4 toughness). Kraven
        // stays alive on the battlefield, so when the dies trigger resolves it can also
        // place the +1/+1 counter — both halves of the effect are observable.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val kraven = driver.putCreatureOnBattlefield(ap, "Kraven the Hunter")
        val attacker = driver.putCreatureOnBattlefield(ap, "Test Beast 6/6")
        val opBlocker = driver.putCreatureOnBattlefield(op, "Test Beast 4/4")
        driver.removeSummoningSickness(attacker)

        val handBefore = driver.getHandSize(ap)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(ap, listOf(attacker), op)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(op, mapOf(opBlocker to listOf(attacker)))
        driver.passPriorityUntil(Step.END_COMBAT)
        // Resolve Kraven's queued trigger.
        repeat(4) { driver.bothPass() }

        kravenCounters(driver, kraven) shouldBe 1
        driver.getHandSize(ap) shouldBe handBefore + 1
    }

    test("stolen creature dies under thief's control — Kraven fires using last-known controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))

        val ap = driver.activePlayer!!
        val op = driver.getOpponent(ap)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // AP controls Kraven. AP owns a 4/4; OP has stolen it (e.g., Threaten) so OP
        // controls it at this instant. From AP's perspective, the dying creature is
        // controlled by an opponent (OP). The "creatures that player controls" group
        // is OP's creatures: just the 4/4 itself (alone, since AP owns no others on the
        // battlefield under OP's control). It qualifies as the greatest.
        val kraven = driver.putCreatureOnBattlefield(ap, "Kraven the Hunter")
        val stolen = driver.putCreatureOnBattlefield(ap, "Test Beast 4/4")
        // Re-assign control to OP — simulating Threaten / Mind Control. Owner stays AP.
        val newState = driver.state.updateEntity(stolen) { container ->
            container.with(ControllerComponent(op))
        }
        driver.replaceState(newState)

        val handBefore = driver.getHandSize(ap)

        killWithDoomBlade(driver, ap, stolen)

        // Without the lastKnownController plumbing, the engine would treat ownerId=AP as
        // the controller, fail the opponentControls() filter (AP is not AP's opponent),
        // and silently skip the trigger.
        kravenCounters(driver, kraven) shouldBe 1
        driver.getHandSize(ap) shouldBe handBefore + 1
    }
})
