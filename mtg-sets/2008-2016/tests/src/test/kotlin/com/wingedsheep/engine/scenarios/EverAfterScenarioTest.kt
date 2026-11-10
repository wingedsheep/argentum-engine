package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.EverAfter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Ever After (SOI #109) — "Return up to two target creature cards from your graveyard to the
 * battlefield. Each of those creatures is a black Zombie in addition to its other colors and types.
 * Put Ever After on the bottom of its owner's library."
 *
 * Pins that the returned creatures keep their own colours and creature types while gaining black and
 * Zombie, that "up to two" accepts a single target, and that Ever After ends on the bottom of its
 * owner's library rather than in the graveyard.
 */
class EverAfterScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(EverAfter))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castEverAfter(targets: List<EntityId>): EntityId {
        val me = activePlayer!!
        val spell = putCardInHand(me, "Ever After")
        giveMana(me, Color.BLACK, 6)
        submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = targets.map { ChosenTarget.Card(it, me, Zone.GRAVEYARD) },
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        bothPass()
        return spell
    }

    test("two creature cards return as black Zombies in addition to their colours and types") {
        val d = setup()
        val me = d.activePlayer!!
        val lions = d.putCardInGraveyard(me, "Savannah Lions")
        val courser = d.putCardInGraveyard(me, "Centaur Courser")

        val spell = d.castEverAfter(listOf(lions, courser))

        val projected = d.state.projectedState
        (lions in d.state.getBattlefield()) shouldBe true
        (courser in d.state.getBattlefield()) shouldBe true
        projected.getColors(lions) shouldBe setOf("WHITE", "BLACK")
        projected.getColors(courser) shouldBe setOf("GREEN", "BLACK")
        projected.getSubtypes(lions) shouldContainAll setOf("Cat", "Zombie")
        projected.getSubtypes(courser) shouldContainAll setOf("Centaur", "Warrior", "Zombie")

        (spell in d.state.getGraveyard(me)) shouldBe false
        d.state.getLibrary(me).last() shouldBe spell
    }

    test("up to two: a single target returns alone and Ever After still goes to the library bottom") {
        val d = setup()
        val me = d.activePlayer!!
        val lions = d.putCardInGraveyard(me, "Savannah Lions")
        val courser = d.putCardInGraveyard(me, "Centaur Courser")

        val spell = d.castEverAfter(listOf(lions))

        (lions in d.state.getBattlefield()) shouldBe true
        d.state.projectedState.getSubtypes(lions) shouldContain "Zombie"
        (courser in d.state.getGraveyard(me)) shouldBe true
        d.state.getLibrary(me).last() shouldBe spell
    }
})
