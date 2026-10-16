package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.Vault112SadisticSimulation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Vault 112: Sadistic Simulation (PIP #123), chapter III: the {E} paid is a player-chosen amount
 * that becomes the number of cards exiled from the top. One of them may be played for free while
 * the chapter resolves, and the rest stay in exile. Paying zero exiles nothing.
 */
class Vault112SadisticSimulationScenarioTest : FunSpec({
    // Run the real chapter III effect as a sorcery so the test doesn't walk turns through draw steps.
    val chapterThree = card("Vault 112 Chapter III Probe") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell { effect = Vault112SadisticSimulation.sagaChapters.single { it.chapter == 3 }.effect }
    }
    val energyProbe = card("Vault 112 Energy Probe") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.GetEnergy(3) }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Vault112SadisticSimulation, chapterThree, energyProbe))
        initMirrorMatch(Deck.of("Grizzly Bears" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.cast(probe: String, color: Color) {
        val me = activePlayer!!
        val spell = putCardInHand(me, probe)
        giveMana(me, color, 1)
        castSpell(me, spell).error shouldBe null
        bothPass()
    }
    fun GameTestDriver.energy(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.payEnergy(amount: Int) {
        val decision = pendingDecision as ChooseNumberDecision
        decision.minValue shouldBe 0
        decision.maxValue shouldBe 3
        submitDecision(activePlayer!!, NumberChosenResponse(decision.id, amount)).error shouldBe null
    }
    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (getStackSpellNames().isNotEmpty() && guard++ < 10) bothPass()
    }

    test("paying two energy exiles two cards; the one chosen is cast for free, the other stays exiled") {
        val d = driver()
        val me = d.activePlayer!!
        d.cast(energyProbe.name, Color.RED)
        d.energy(me) shouldBe 3
        val librarySize = d.state.getZone(me, Zone.LIBRARY).size

        d.cast(chapterThree.name, Color.BLUE)
        d.payEnergy(2)

        d.energy(me) shouldBe 1
        val exiled = d.state.getZone(me, Zone.EXILE)
        exiled.size shouldBe 2
        d.state.getZone(me, Zone.LIBRARY).size shouldBe librarySize - 2

        val choice = d.pendingDecision as SelectCardsDecision
        choice.minSelections shouldBe 0
        choice.maxSelections shouldBe 1
        choice.options.toSet() shouldBe exiled.toSet()
        val bears = exiled.first()
        d.submitCardSelection(me, listOf(bears)).error shouldBe null
        d.resolveAll()

        d.getPermanents(me).contains(bears) shouldBe true
        d.state.getZone(me, Zone.EXILE) shouldBe exiled.filterNot { it == bears }
    }

    test("declining the free play leaves every exiled card in exile") {
        val d = driver()
        val me = d.activePlayer!!
        d.cast(energyProbe.name, Color.RED)
        d.cast(chapterThree.name, Color.BLUE)
        d.payEnergy(3)

        d.submitCardSelection(me, emptyList()).error shouldBe null
        d.resolveAll()

        d.energy(me) shouldBe 0
        d.state.getZone(me, Zone.EXILE).size shouldBe 3
        d.getCreatures(me) shouldBe emptyList()
    }

    test("paying zero energy exiles nothing and keeps the energy") {
        val d = driver()
        val me = d.activePlayer!!
        d.cast(energyProbe.name, Color.RED)
        val librarySize = d.state.getZone(me, Zone.LIBRARY).size

        d.cast(chapterThree.name, Color.BLUE)
        d.payEnergy(0)

        d.pendingDecision shouldBe null
        d.energy(me) shouldBe 3
        d.state.getZone(me, Zone.EXILE).size shouldBe 0
        d.state.getZone(me, Zone.LIBRARY).size shouldBe librarySize
    }
})
