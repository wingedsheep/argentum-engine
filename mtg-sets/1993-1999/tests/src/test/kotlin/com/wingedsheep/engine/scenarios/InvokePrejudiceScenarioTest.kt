package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.InvokePrejudice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invoke Prejudice — "Whenever an opponent casts a creature spell that doesn't share a color with
 * a creature you control, counter that spell unless that player pays {X}, where X is its mana
 * value."
 *
 * Pins the cast-time color comparison against the enchantment controller's creatures (not the
 * caster's), the caster as payer, and X read off the triggering spell.
 */
class InvokePrejudiceScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(InvokePrejudice))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("an off-color creature spell the caster can't pay {X} for is countered") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putPermanentOnBattlefield(owner, "Invoke Prejudice")
        d.putPermanentOnBattlefield(owner, "Savannah Lions")

        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        d.castSpell(caster, bears).error shouldBe null
        d.stackSize shouldBe 2
        d.bothPass()

        // {2} is unaffordable with an empty pool and no lands, so the caster isn't asked.
        d.pendingDecision shouldBe null
        d.stackSize shouldBe 0
        d.getGraveyardCardNames(caster) shouldContain "Grizzly Bears"
    }

    test("the caster may pay {X} = the spell's mana value to let it resolve") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putPermanentOnBattlefield(owner, "Invoke Prejudice")
        d.putPermanentOnBattlefield(owner, "Savannah Lions")

        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 4)
        d.castSpell(caster, bears).error shouldBe null
        d.stackSize shouldBe 2
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe caster
        d.submitYesNo(caster, true).error shouldBe null
        d.stackSize shouldBe 1
        d.bothPass()
        d.findPermanent(caster, "Grizzly Bears") shouldNotBe null
    }

    test("a creature spell sharing a color with a creature the enchantment's controller controls doesn't trigger it") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putPermanentOnBattlefield(owner, "Invoke Prejudice")
        d.putPermanentOnBattlefield(owner, "Grizzly Bears")

        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        d.castSpell(caster, bears).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("the caster's own creatures don't count: a green caster with green creatures still triggers it") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putPermanentOnBattlefield(owner, "Invoke Prejudice")
        d.putPermanentOnBattlefield(owner, "Savannah Lions")
        d.putPermanentOnBattlefield(caster, "Grizzly Bears")

        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        d.castSpell(caster, bears).error shouldBe null
        d.stackSize shouldBe 2
    }

    test("a colorless creature spell shares no color, so it always triggers") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putPermanentOnBattlefield(owner, "Invoke Prejudice")
        d.putPermanentOnBattlefield(owner, "Grizzly Bears")

        val golem = d.putCardInHand(caster, "Artifact Creature")
        d.giveColorlessMana(caster, 2)
        d.castSpell(caster, golem).error shouldBe null
        d.stackSize shouldBe 2
        d.bothPass()
        d.getGraveyardCardNames(caster) shouldContain "Artifact Creature"
    }

    test("its controller's own creature spells never trigger it") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Invoke Prejudice")

        val bears = d.putCardInHand(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, bears).error shouldBe null
        d.stackSize shouldBe 1
    }
})
