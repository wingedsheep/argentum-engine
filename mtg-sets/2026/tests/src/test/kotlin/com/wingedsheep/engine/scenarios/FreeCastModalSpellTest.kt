package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.mtg.sets.definitions.ddj.cards.IzzetCharm
import com.wingedsheep.mtg.sets.definitions.ecl.cards.PyrrhicStrike
import com.wingedsheep.mtg.sets.definitions.lrw.cards.CrypticCommand
import com.wingedsheep.mtg.sets.definitions.m13.cards.Omniscience
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
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

    fun EnumerationTestDriver.bf(name: String): EntityId =
        game.state.getZone(ZoneKey(player1, Zone.BATTLEFIELD))
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

    test("the free blight cast resolves both modes and blights the fodder") {
        val driver = setup("Pyrrhic Strike")
        val p1 = driver.player1
        val fodder = driver.bf("Force of Nature")
        val enchantment = driver.bf("Test Enchantment")
        val creature = driver.bf("Centaur Courser")
        val blight = driver.enumerateFor(p1).castActionsFor("Pyrrhic Strike")
            .single { it.actionType == "CastSpellModal" }

        driver.game.submit(
            (blight.action as CastSpell).copy(
                chosenModes = listOf(0, 1),
                additionalCostPayment = AdditionalCostPayment(blightAmount = 2, blightTargets = listOf(fodder)),
            )
        ).error shouldBe null

        var safety = 0
        while (driver.game.pendingDecision is ChooseTargetsDecision && safety < 10) {
            val decision = driver.game.pendingDecision as ChooseTargetsDecision
            val req = decision.targetRequirements.first()
            val legal = decision.legalTargets[req.index].orEmpty()
            val pick = listOf(enchantment, creature).first { it in legal }
            driver.game.submitDecision(p1, TargetsResponse(decision.id, mapOf(req.index to listOf(pick))))
            safety++
        }
        safety = 0
        while (driver.game.stackSize > 0 && safety < 20) { driver.game.bothPass(); safety++ }

        driver.game.findPermanent(p1, "Test Enchantment") shouldBe null
        driver.game.findPermanent(p1, "Centaur Courser") shouldBe null
        driver.game.state.getEntity(fodder)?.get<CountersComponent>()
            ?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
    }
})
