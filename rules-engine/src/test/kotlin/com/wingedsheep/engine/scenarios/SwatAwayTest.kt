package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.SwatAway
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Swat Away.
 *
 * {2}{U}{U} Instant
 * This spell costs {2} less to cast if a creature is attacking you.
 * The owner of target spell or creature puts it on their choice of the top or
 * bottom of their library.
 *
 * Exercises:
 *  - the [com.wingedsheep.sdk.scripting.CostReductionSource.FixedIfCreatureAttackingYou]
 *    cost source (new).
 *  - the spell-on-stack branch of [com.wingedsheep.sdk.scripting.effects.PutOnTopOrBottomOfLibraryEffect]
 *    (extended).
 *  - the permanent-filter branch of [com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent]
 *    (creature only).
 */
class SwatAwayTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SwatAway)
        return driver
    }

    /** Stamp an attack directly onto a creature, mimicking DeclareAttackers having resolved. */
    fun markAttacking(driver: GameTestDriver, attackerId: EntityId, defenderId: EntityId) {
        driver.replaceState(
            driver.state.updateEntity(attackerId) { c ->
                c.with(AttackingComponent(defenderId = defenderId))
            }
        )
    }

    test("cost reduction applies when a creature is attacking you — {U}{U} is enough") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val swatAway = driver.putCardInHand(p1, "Swat Away")
        val attacker = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        markAttacking(driver, attacker, p1)

        // Only {U}{U} available — cost reduction must kick in to make it castable.
        driver.giveMana(p1, Color.BLUE, 2)

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = swatAway,
                targets = listOf(ChosenTarget.Permanent(attacker)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.stackSize shouldBe 1
    }

    test("cost reduction does not apply when no creature is attacking — spell can't be cast on {U}{U}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val swatAway = driver.putCardInHand(p1, "Swat Away")
        val opposingCreature = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        // No AttackingComponent — opposingCreature is idle on the battlefield.

        driver.giveMana(p1, Color.BLUE, 2)

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = swatAway,
                targets = listOf(ChosenTarget.Permanent(opposingCreature)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }

    test("targeting a creature on the battlefield — owner chooses top of library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val swatAway = driver.putCardInHand(p1, "Swat Away")
        val attacker = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        markAttacking(driver, attacker, p1)

        driver.giveMana(p1, Color.BLUE, 2)

        driver.submit(
            CastSpell(
                playerId = p1,
                cardId = swatAway,
                targets = listOf(ChosenTarget.Permanent(attacker)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve Swat Away; top-or-bottom decision pauses execution.

        val decision = driver.pendingDecision
        (decision is ChooseOptionDecision) shouldBe true
        decision as ChooseOptionDecision
        decision.playerId shouldBe p2 // creature's owner chooses
        decision.options shouldBe listOf("Top of library", "Bottom of library")

        driver.submit(SubmitDecision(p2, OptionChosenResponse(decision.id, 0))) // Top

        driver.isPaused shouldBe false
        driver.findPermanent(p2, "Centaur Courser") shouldBe null
        val p2Library = driver.state.getZone(ZoneKey(p2, Zone.LIBRARY))
        p2Library.first() shouldBe attacker
    }

    test("targeting a creature on the battlefield — owner chooses bottom of library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val swatAway = driver.putCardInHand(p1, "Swat Away")
        val attacker = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        markAttacking(driver, attacker, p1)

        driver.giveMana(p1, Color.BLUE, 2)

        driver.submit(
            CastSpell(
                playerId = p1,
                cardId = swatAway,
                targets = listOf(ChosenTarget.Permanent(attacker)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        driver.submit(SubmitDecision(p2, OptionChosenResponse(decision.id, 1))) // Bottom

        driver.findPermanent(p2, "Centaur Courser") shouldBe null
        val p2Library = driver.state.getZone(ZoneKey(p2, Zone.LIBRARY))
        p2Library.last() shouldBe attacker
    }

    test("targeting a spell on the stack — spell is removed from stack and put on library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 passes priority so P2 can cast at instant speed.
        val bolt = driver.putCardInHand(p2, "Lightning Bolt")
        driver.giveMana(p2, Color.RED, 1)
        driver.passPriority(p1)

        driver.submit(
            CastSpell(
                playerId = p2,
                cardId = bolt,
                targets = listOf(ChosenTarget.Player(p1)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        val boltOnStack = driver.getTopOfStack()!!
        driver.stackSize shouldBe 1

        // P2 passes priority back to P1 so P1 can respond.
        driver.passPriority(p2)

        // P1 casts Swat Away targeting the Lightning Bolt (full mana, no reduction needed).
        val swatAway = driver.putCardInHand(p1, "Swat Away")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.giveColorlessMana(p1, 2)

        val cast = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = swatAway,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        cast.isSuccess shouldBe true
        driver.stackSize shouldBe 2

        // Resolve Swat Away — Lightning Bolt's owner (p2) gets the top/bottom choice.
        driver.bothPass()
        val decision = driver.pendingDecision
        (decision is ChooseOptionDecision) shouldBe true
        decision as ChooseOptionDecision
        decision.playerId shouldBe p2
        driver.submit(SubmitDecision(p2, OptionChosenResponse(decision.id, 0))) // Top

        // The Lightning Bolt is off the stack and on top of p2's library.
        driver.state.stack.contains(boltOnStack) shouldBe false
        val p2Library = driver.state.getZone(ZoneKey(p2, Zone.LIBRARY))
        p2Library shouldContain boltOnStack
        p2Library.first() shouldBe boltOnStack
    }
})
