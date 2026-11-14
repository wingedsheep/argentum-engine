package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.RenownedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.ScabClanBerserker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scab-Clan Berserker (ORI #160) — {1}{R}{R} Creature — Human Berserker 2/2.
 *
 *   Haste
 *   Renown 1
 *   Whenever an opponent casts a noncreature spell, if this creature is renowned, this creature
 *   deals 2 damage to that player.
 *
 * Pins the intervening-"if" on `Conditions.SourceIsRenowned` for a cast trigger, the noncreature
 * filter, the opponent-only scope, and that "that player" is the caster.
 */
class ScabClanBerserkerScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ScabClanBerserker))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.berserker(controller: EntityId, renowned: Boolean): EntityId {
        val berserker = putPermanentOnBattlefield(controller, "Scab-Clan Berserker")
        if (renowned) replaceState(state.updateEntity(berserker) { it.with(RenownedComponent) })
        return berserker
    }

    test("renowned: an opponent's noncreature spell makes the Berserker deal 2 damage to its caster") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.berserker(owner, renowned = true)

        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, targets = listOf(owner)).error shouldBe null
        withClue("the trigger goes on the stack above the Bolt") { d.stackSize shouldBe 2 }

        d.bothPass()
        withClue("the caster takes 2 from the trigger") { d.getLifeTotal(caster) shouldBe 18 }
        d.getLifeTotal(owner) shouldBe 20

        d.bothPass()
        d.getLifeTotal(owner) shouldBe 17
    }

    test("not renowned: the intervening-if fails and nothing triggers") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.berserker(owner, renowned = false)

        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, targets = listOf(owner)).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("renowned: an opponent's creature spell doesn't trigger it") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.berserker(owner, renowned = true)

        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        d.castSpell(caster, bears).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("renowned: its controller's own noncreature spell doesn't trigger it") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        d.berserker(me, renowned = true)

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, targets = listOf(opponent)).error shouldBe null
        d.stackSize shouldBe 1
    }
})
