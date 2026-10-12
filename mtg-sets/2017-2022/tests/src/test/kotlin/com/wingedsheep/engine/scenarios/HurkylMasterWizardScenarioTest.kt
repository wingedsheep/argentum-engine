package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.HurkylMasterWizard
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.ZoneKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hurkyl, Master Wizard (BRO #51) — at your end step, if you've cast a noncreature spell this turn,
 * reveal the top five; for each card type among noncreature spells you've cast this turn you may
 * put a card of that type into your hand; the rest go to the bottom in a random order.
 *
 * A Kindred Instant counts as both an instant and a kindred spell, so it opens two slots, and a
 * revealed card fills at most one of them.
 */
class HurkylMasterWizardScenarioTest : FunSpec({

    val kindredBolt = card("Test Kindred Bolt") {
        manaCost = "{U}"
        typeLine = "Kindred Instant — Wizard"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HurkylMasterWizard, kindredBolt))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(caster: EntityId, name: String) {
        giveMana(caster, Color.WHITE, 2)
        giveMana(caster, Color.BLUE, 1)
        val spell = putCardInHand(caster, name)
        submit(CastSpell(playerId = caster, cardId = spell)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.library(player: EntityId) = state.getZone(ZoneKey(player, Zone.LIBRARY))

    test("each cast card type opens one pick; a revealed card fills only one slot; uncast types are skipped") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Hurkyl, Master Wizard")

        driver.cast(me, "Test Enchantment")
        driver.cast(me, "Test Kindred Bolt")

        // Last put is the top card: the top five are kindred, bolt, sorcery, enchantment, bears.
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val enchantment = driver.putCardOnTopOfLibrary(me, "Test Enchantment")
        val sorcery = driver.putCardOnTopOfLibrary(me, "Doom Blade")
        val bolt = driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val kindred = driver.putCardOnTopOfLibrary(me, "Test Kindred Bolt")

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        val enchantmentPick = driver.pendingDecision
        enchantmentPick.shouldBeInstanceOf<SelectCardsDecision>()
        enchantmentPick.options shouldBe listOf(enchantment)
        driver.submitCardSelection(me, listOf(enchantment)).error shouldBe null

        // The kindred instant is an instant too, so both instants are offered here.
        val instantPick = driver.pendingDecision
        instantPick.shouldBeInstanceOf<SelectCardsDecision>()
        instantPick.options.shouldContainExactlyInAnyOrder(kindred, bolt)
        driver.submitCardSelection(me, listOf(bolt)).error shouldBe null

        val kindredPick = driver.pendingDecision
        kindredPick.shouldBeInstanceOf<SelectCardsDecision>()
        kindredPick.options shouldBe listOf(kindred)
        driver.submitCardSelection(me, listOf(kindred)).error shouldBe null

        // No artifact, battle, planeswalker or sorcery spell was cast: Doom Blade is never offered.
        driver.pendingDecision shouldBe null
        driver.getHand(me).shouldContainAll(enchantment, bolt, kindred)
        driver.library(me).takeLast(2).shouldContainExactlyInAnyOrder(sorcery, bears)
    }

    test("a card picked for one type isn't offered again for another") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Hurkyl, Master Wizard")

        driver.cast(me, "Test Kindred Bolt")

        repeat(4) { driver.putCardOnTopOfLibrary(me, "Grizzly Bears") }
        val kindred = driver.putCardOnTopOfLibrary(me, "Test Kindred Bolt")

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        val instantPick = driver.pendingDecision
        instantPick.shouldBeInstanceOf<SelectCardsDecision>()
        instantPick.options shouldBe listOf(kindred)
        driver.submitCardSelection(me, listOf(kindred)).error shouldBe null

        // The kindred slot has nothing left to offer, so it asks nothing.
        driver.pendingDecision shouldBe null
        driver.getHand(me).shouldContainAll(kindred)
    }

    test("no trigger when only a creature spell was cast") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Hurkyl, Master Wizard")

        driver.giveMana(me, Color.GREEN, 2)
        val bearsSpell = driver.putCardInHand(me, "Grizzly Bears")
        driver.submit(CastSpell(playerId = me, cardId = bearsSpell)).error shouldBe null
        driver.bothPass()

        val topBefore = driver.library(me).take(5)
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.library(me).take(5) shouldBe topBefore
    }
})
