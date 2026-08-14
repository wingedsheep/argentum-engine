package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.ReasonableDoubt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Reasonable Doubt (MKM #69) — {1}{U} Instant.
 *
 * "Counter target spell unless its controller pays {2}. Suspect up to one target creature."
 *
 * Two independent targets on one spell, so the risky part is *ordering*: the counter reads the
 * spell off the head of the target list, while the optional creature must occupy the last slot.
 * These cover both halves firing together, the optional target being declined, and the two
 * sentences being genuinely independent (paying {2} still leaves the creature suspected).
 */
class ReasonableDoubtScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ReasonableDoubt))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("counters the spell and suspects the creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        val doubt = driver.putCardInHand(me, "Reasonable Doubt")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = doubt,
                targets = listOf(ChosenTarget.Spell(boltOnStack), ChosenTarget.Permanent(bear)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // Enough floating mana that declining is a real choice rather than an auto-counter.
        driver.giveColorlessMana(opponent, 2)

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(opponent, false)

        withClue("declining {2} counters the spell") {
            driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
        }
        withClue("the second sentence resolves too — the creature is suspected") {
            StateProjector().project(driver.state).isSuspected(bear) shouldBe true
        }
    }

    test("paying {2} saves the spell but the creature is still suspected") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        val doubt = driver.putCardInHand(me, "Reasonable Doubt")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = doubt,
                targets = listOf(ChosenTarget.Spell(boltOnStack), ChosenTarget.Permanent(bear)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        driver.giveColorlessMana(opponent, 2)

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(opponent, true)

        withClue("the two sentences are independent — paying doesn't spare the creature") {
            StateProjector().project(driver.state).isSuspected(bear) shouldBe true
        }
        // Lightning Bolt was not countered — it resolves and deals 3.
        driver.bothPass()
        driver.assertLifeTotal(me, 17)
    }

    test("the creature target is optional — the counter still works alone") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        val doubt = driver.putCardInHand(me, "Reasonable Doubt")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = doubt,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        driver.bothPass()
        if (driver.pendingDecision is YesNoDecision) driver.submitYesNo(opponent, false)

        withClue("no creature chosen — the counter half is unaffected") {
            driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
        }
        withClue("nothing was suspected") {
            StateProjector().project(driver.state).isSuspected(bear) shouldBe false
        }
    }

    test("an unpayable {2} counters without prompting") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        val doubt = driver.putCardInHand(me, "Reasonable Doubt")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = doubt,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        driver.bothPass()

        withClue("the opponent is tapped out, so there is nothing to decide") {
            driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
            driver.getGraveyardCardNames(me) shouldNotContain "Lightning Bolt"
        }
    }
})
