package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.MagnanimousMagistrate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Magnanimous Magistrate (J22) —
 * This creature enters with five reprieve counters on it.
 * Whenever another nontoken creature you control dies, if its mana value was 1 or greater, you may
 * remove that many reprieve counters from this creature. If you do, return that card to the
 * battlefield under its owner's control.
 */
class MagnanimousMagistrateScenarioTest : FunSpec({

    // A mana-value-0 nontoken creature (Ornithopter's shape) — the intervening-if's floor.
    val zeroDrop = CardDefinition.creature(
        name = "Zero Drop",
        manaCost = ManaCost.ZERO,
        subtypes = setOf(Subtype("Thopter")),
        power = 0,
        toughness = 2
    )

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + MagnanimousMagistrate + zeroDrop)
        initMirrorMatch(deck = Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.reprieve(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.REPRIEVE) ?: 0

    fun GameTestDriver.magistrateWith(you: EntityId, counters: Int): EntityId =
        putCreatureOnBattlefield(you, "Magnanimous Magistrate").also {
            addComponent(it, CountersComponent(mapOf(CounterType.REPRIEVE to counters)))
        }

    /** Bolt [victim] and run the stack, answering the Magistrate's "you may" with [accept]. */
    fun GameTestDriver.boltAndResolve(you: EntityId, victim: EntityId, accept: Boolean): Boolean {
        val bolt = putCardInHand(you, "Lightning Bolt")
        giveMana(you, Color.RED, 1)
        submit(
            CastSpell(you, bolt, targets = listOf(ChosenTarget.Permanent(victim)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        var asked = false
        var guard = 0
        while (guard++ < 20 && (state.stack.isNotEmpty() || pendingDecision != null)) {
            if (pendingDecision is YesNoDecision) {
                asked = true
                submitYesNo(you, accept)
            } else {
                bothPass()
            }
        }
        return asked
    }

    test("it enters with five reprieve counters") {
        val d = setup()
        val you = d.activePlayer!!
        val magistrate = d.putCardInHand(you, "Magnanimous Magistrate")
        d.giveMana(you, Color.WHITE, 6)

        d.submit(CastSpell(you, magistrate, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        while (d.state.stack.isNotEmpty()) d.bothPass()

        d.reprieve(magistrate) shouldBe 5
    }

    test("removing counters equal to the dead creature's mana value returns it") {
        val d = setup()
        val you = d.activePlayer!!
        val magistrate = d.magistrateWith(you, 5)
        val courser = d.putCreatureOnBattlefield(you, "Centaur Courser") // {2}{G}, MV 3

        d.boltAndResolve(you, courser, accept = true) shouldBe true

        d.reprieve(magistrate) shouldBe 2
        d.state.getBattlefield() shouldContain courser
        d.getGraveyard(you) shouldNotContain courser
    }

    test("declining keeps the counters and leaves the card in the graveyard") {
        val d = setup()
        val you = d.activePlayer!!
        val magistrate = d.magistrateWith(you, 5)
        val courser = d.putCreatureOnBattlefield(you, "Centaur Courser")

        d.boltAndResolve(you, courser, accept = false) shouldBe true

        d.reprieve(magistrate) shouldBe 5
        d.getGraveyard(you) shouldContain courser
    }

    test("too few reprieve counters offers no choice, removes none, and returns nothing") {
        val d = setup()
        val you = d.activePlayer!!
        val magistrate = d.magistrateWith(you, 2)
        val courser = d.putCreatureOnBattlefield(you, "Centaur Courser")

        d.boltAndResolve(you, courser, accept = true) shouldBe false

        d.reprieve(magistrate) shouldBe 2
        d.getGraveyard(you) shouldContain courser
    }

    test("an opponent's creature dying doesn't trigger it") {
        val d = setup()
        val you = d.activePlayer!!
        val magistrate = d.magistrateWith(you, 5)
        val enemy = d.putCreatureOnBattlefield(d.getOpponent(you), "Centaur Courser")

        d.boltAndResolve(you, enemy, accept = true) shouldBe false

        d.reprieve(magistrate) shouldBe 5
        d.getGraveyard(d.getOpponent(you)) shouldContain enemy
    }

    test("a mana-value-0 creature dying doesn't trigger it, so it can't return for free") {
        val d = setup()
        val you = d.activePlayer!!
        val magistrate = d.magistrateWith(you, 5)
        val thopter = d.putCreatureOnBattlefield(you, "Zero Drop")

        d.boltAndResolve(you, thopter, accept = true) shouldBe false

        d.reprieve(magistrate) shouldBe 5
        d.getGraveyard(you) shouldContain thopter
    }
})
