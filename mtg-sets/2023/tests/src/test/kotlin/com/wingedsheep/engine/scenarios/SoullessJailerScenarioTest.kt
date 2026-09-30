package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.SqueeTheImmortal
import com.wingedsheep.mtg.sets.definitions.ody.cards.Zombify
import com.wingedsheep.mtg.sets.definitions.one.cards.SoullessJailer
import com.wingedsheep.mtg.sets.definitions.tsp.cards.ThinkTwice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Soulless Jailer (ONE #241) — {2} Artifact Creature — Phyrexian Golem, 0/4.
 *
 * "Permanent cards in graveyards can't enter the battlefield.
 *  Players can't cast noncreature spells from graveyards or exile."
 */
class SoullessJailerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(SoullessJailer, Zombify, ThinkTwice, SqueeTheImmortal))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castableCards(player: EntityId): Set<EntityId> =
        legalActions(player).filter { it.affordable }.mapNotNull { (it.action as? CastSpell)?.cardId }.toSet()

    test("a creature card can't be returned from a graveyard to the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        driver.putCreatureOnBattlefield(me, "Soulless Jailer")
        val courser = driver.putCardInGraveyard(me, "Centaur Courser")
        val zombify = driver.putCardInHand(me, "Zombify")

        driver.giveMana(me, Color.BLACK, 4)
        driver.castSpellWithTargets(me, zombify, listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD)))
        driver.bothPass()

        driver.getGraveyard(me) shouldContain courser
        driver.findPermanent(me, "Centaur Courser") shouldBe null
    }

    test("the entry lock lifts once the Jailer leaves") {
        val driver = newDriver()
        val me = driver.player1
        val jailer = driver.putCreatureOnBattlefield(me, "Soulless Jailer")
        val courser = driver.putCardInGraveyard(me, "Centaur Courser")
        driver.moveToGraveyard(jailer)
        val zombify = driver.putCardInHand(me, "Zombify")

        driver.giveMana(me, Color.BLACK, 4)
        driver.castSpellWithTargets(me, zombify, listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD)))
        driver.bothPass()

        driver.findPermanent(me, "Centaur Courser") shouldBe courser
    }

    test("noncreature spells can't be cast from a graveyard or exile; creature spells and the hand are unaffected") {
        val driver = newDriver()
        val me = driver.player1
        driver.giveMana(me, Color.BLUE, 4)
        driver.giveMana(me, Color.RED, 2)
        val flashback = driver.putCardInGraveyard(me, "Think Twice")
        val fromHand = driver.putCardInHand(me, "Think Twice")
        val squeeInGraveyard = driver.putCardInGraveyard(me, "Squee, the Immortal")
        val squeeInExile = driver.putCardInExile(me, "Squee, the Immortal")

        val flashbackCast = driver.legalActions(me).map { it.action }
            .filterIsInstance<CastSpell>().first { it.cardId == flashback }

        val jailer = driver.putCreatureOnBattlefield(me, "Soulless Jailer")
        val locked = driver.castableCards(me)
        locked shouldNotContain flashback
        locked shouldContain fromHand
        locked shouldContain squeeInGraveyard
        locked shouldContain squeeInExile
        // The handler refuses it too, not only the legal-action list.
        driver.submitExpectFailure(flashbackCast)

        driver.moveToGraveyard(jailer)
        driver.castableCards(me) shouldContain flashback
    }
})
