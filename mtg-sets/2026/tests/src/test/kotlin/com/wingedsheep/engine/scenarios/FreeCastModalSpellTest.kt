package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.mtg.sets.definitions.ddj.cards.IzzetCharm
import com.wingedsheep.mtg.sets.definitions.ecl.cards.PyrrhicStrike
import com.wingedsheep.mtg.sets.definitions.lrw.cards.CrypticCommand
import com.wingedsheep.mtg.sets.definitions.m13.cards.Omniscience
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A "cast without paying its mana cost" permission (Omniscience) applies to modal spells too: the
 * caster still chooses modes (CR 700.2a), only the mana cost is waived. The modal cast-variant
 * builder used to emit only the paid and blight variants, so a modal spell in hand under
 * Omniscience with no mana available was simply not castable.
 */
class FreeCastModalSpellTest : FunSpec({

    fun setup(spell: String): EnumerationTestDriver = setupP1(
        hand = listOf(spell),
        // No lands: the paid cast is unaffordable, so only the free variants can be on offer.
        battlefield = listOf("Omniscience", "Centaur Courser", "Force of Nature", "Test Enchantment"),
        extraSetCards = listOf(Omniscience, IzzetCharm, CrypticCommand, PyrrhicStrike),
    )

    fun EnumerationTestDriver.handCard(name: String): EntityId =
        game.state.getZone(ZoneKey(player1, Zone.HAND))
            .first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("a choose-one spell offers each castable mode for free") {
        val driver = setup("Izzet Charm")
        val casts = driver.enumerateFor(driver.player1).castActionsFor("Izzet Charm")

        // "Counter target noncreature spell" has nothing to target, so two modes remain.
        casts.map { it.actionType }.distinct() shouldBe listOf("CastSpellMode")
        casts.map { (it.action as CastSpell).chosenModes } shouldContainExactlyInAnyOrder listOf(listOf(1), listOf(2))
        casts.all { (it.action as CastSpell).useWithoutPayingManaCost } shouldBe true
        casts.all { it.description.endsWith("(Free)") } shouldBe true
    }

    test("the free choose-one cast resolves the chosen mode") {
        val driver = setup("Izzet Charm")
        val p1 = driver.player1
        val charm = driver.handCard("Izzet Charm")
        val draw = driver.enumerateFor(p1).castActionsFor("Izzet Charm")
            .single { (it.action as CastSpell).chosenModes == listOf(2) }

        driver.game.submit(draw.action).error shouldBe null
        driver.game.state.stack.contains(charm) shouldBe true
    }

    test("a choose-two spell offers one free modal cast that the handler accepts") {
        val driver = setup("Cryptic Command")
        val p1 = driver.player1
        val modal = driver.enumerateFor(p1).castActionsFor("Cryptic Command").single()

        modal.actionType shouldBe "CastSpellModal"
        (modal.action as CastSpell).useWithoutPayingManaCost shouldBe true
        modal.modalEnumeration.shouldNotBeNull().chooseCount shouldBe 2

        // Tap all opponents' creatures + draw a card: no targets, so no follow-up prompt.
        val command = driver.handCard("Cryptic Command")
        driver.game.submit((modal.action as CastSpell).copy(chosenModes = listOf(2, 3))).error shouldBe null
        driver.game.state.stack.contains(command) shouldBe true
    }

    test("blight stays payable on a free cast and still unlocks both modes") {
        val driver = setup("Pyrrhic Strike")
        val casts = driver.enumerateFor(driver.player1).castActionsFor("Pyrrhic Strike")

        casts.filter { !(it.action as CastSpell).useWithoutPayingManaCost }.shouldBeEmpty()
        casts.count { it.actionType == "CastSpellMode" } shouldBe 2
        val blight = casts.single { it.actionType == "CastSpellModal" }
        blight.additionalCostInfo.shouldNotBeNull().costType shouldBe "Blight"
        blight.modalEnumeration.shouldNotBeNull().minChooseCount shouldBe 2
    }
})
