package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ScopedManaProductionTest : FunSpec({
    val fixed = card("Scoped Multipart Producer") {
        typeLine = "Snow Land"
        activatedAbility { cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN, 1) then Effects.AddMana(Color.BLUE, 1)
            manaAbility = true }
    }
    val choices = card("Scoped Multipart Choice Producer") {
        typeLine = "Snow Land"
        activatedAbility { cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN, 1) then Effects.AddAnyColorMana(1) then Effects.AddAnyColorMana(1)
            manaAbility = true }
    }
    val split = card("Scoped Dynamic Split Producer") {
        typeLine = "Snow Land"
        activatedAbility { cost = Costs.Tap
            effect = Effects.AddDynamicMana(DynamicAmount.Fixed(3), setOf(Color.RED, Color.GREEN))
            manaAbility = true }
    }
    val pips = card("Scoped Dynamic Pip Producer") {
        typeLine = "Snow Land"
        activatedAbility { cost = Costs.Tap
            effect = Effects.AddDynamicMana(DynamicAmount.Fixed(3), Color.entries.toSet())
            manaAbility = true }
    }
    val zero = card("Scoped Zero Producer") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 0); manaAbility = true }
    }
    val bonus = card("Scoped Tap Bonus") {
        typeLine = "Enchantment"
        staticAbility { ability = AdditionalManaOnSourceTap(GameObjectFilter.Land.youControl(), Color.RED) }
    }
    val damp = card("Scoped Dampening") {
        typeLine = "Artifact"
        staticAbility { ability = DampLandManaProduction }
    }
    val spell = card("Scoped Production Payment") {
        manaCost = "{G}"; typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(fixed, choices, split, pips, zero, bonus, damp, spell))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scope(d: GameTestDriver, pauseAfter: Boolean = false) {
        val p = d.activePlayer!!
        d.replaceState(d.state.pushContinuation(ManaSpendingObligationsContinuation(p,
            EffectContext(sourceId = null, controllerId = p), "scope")))
        if (pauseAfter) d.replaceState(d.state.pushContinuation(EffectContinuation(
            listOf(Effects.May(Effects.GainLife(1))), EffectContext(sourceId = null, controllerId = p))))
    }
    fun pool(d: GameTestDriver) = d.state.getEntity(d.activePlayer!!)!!.get<ManaPoolComponent>()!!
    fun activate(d: GameTestDriver, card: CardDefinition): ExecutionResult {
        val p = d.activePlayer!!; val source = d.putLandOnBattlefield(p, card.name)
        return d.submit(ActivateAbility(p, source, card.script.activatedAbilities.first().id))
    }
    fun roundTrip(d: GameTestDriver) {
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
    }
    fun assertBatch(d: GameTestDriver, count: Int) {
        val ids = d.state.activeManaSpendingScope(d.activePlayer!!)!!.pendingIds
        ids.size shouldBe 1
        pool(d).restrictedMana.size shouldBe count
        pool(d).restrictedMana.all { it.obligationIds == ids && it.source?.isSnow == true } shouldBe true
        d.state.continuationStack.filterIsInstance<ScopedManaProductionContinuation>().size shouldBe 0
    }
    test("fixed multipart production has one snow identity and one aggregate event") {
        val d = driver(); scope(d)
        val result = activate(d, fixed); result.error shouldBe null
        assertBatch(d, 2)
        pool(d).restrictedMana.map { it.color }.toSet() shouldBe setOf(Color.GREEN, Color.BLUE)
        result.events.filterIsInstance<ManaAddedEvent>().single().let { it.green shouldBe 1; it.blue shouldBe 1 }
    }
    test("two serialized color pauses finish production before one separate tap bonus") {
        val d = driver(); val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, bonus.name); scope(d, pauseAfter = true)
        val events = mutableListOf<GameEvent>()
        val result = activate(d, choices); result.error shouldBe null; events += result.events
        repeat(2) { index ->
            pool(d).red shouldBe 0
            d.state.activeManaSpendingScope(p)!!.pendingIds shouldBe emptySet()
            roundTrip(d)
            val decision = d.pendingDecision as ChooseColorDecision
            val answer = d.submitDecision(p, ColorChosenResponse(decision.id, if (index == 0) Color.BLUE else Color.WHITE))
            answer.error shouldBe null; events += answer.events
        }
        assertBatch(d, 3); pool(d).red shouldBe 1
        val production = events.filterIsInstance<ManaAddedEvent>().filter { it.sourceName == choices.name }
        production.size shouldBe 1
        production.single().let { it.green shouldBe 1; it.blue shouldBe 1; it.white shouldBe 1 }
        events.filterIsInstance<LandTappedForManaEvent>().size shouldBe 1
    }
    test("dynamic two-color split retains one identity across save and resume") {
        val d = driver(); scope(d, pauseAfter = true)
        activate(d, split).error shouldBe null; roundTrip(d)
        val decision = d.pendingDecision as ChooseNumberDecision
        d.submitDecision(d.activePlayer!!, NumberChosenResponse(decision.id, 2)).error shouldBe null
        assertBatch(d, 3)
        pool(d).restrictedMana.count { it.color == Color.RED } shouldBe 2
        pool(d).restrictedMana.count { it.color == Color.GREEN } shouldBe 1
    }
    test("pip-by-pip production reports one batch after the final serialized choice") {
        val d = driver(); scope(d, pauseAfter = true)
        val events = mutableListOf<GameEvent>()
        events += activate(d, pips).events
        for (color in listOf(Color.WHITE, Color.BLUE, Color.GREEN)) {
            roundTrip(d)
            val decision = d.pendingDecision as ChooseColorDecision
            val result = d.submitDecision(d.activePlayer!!, ColorChosenResponse(decision.id, color))
            result.error shouldBe null; events += result.events
        }
        assertBatch(d, 3)
        events.filterIsInstance<ManaAddedEvent>().single().let { it.white shouldBe 1; it.blue shouldBe 1; it.green shouldBe 1 }
    }
    for (dampened in listOf(false, true)) {
        test("resumed siblings report only final production with dampening=$dampened") {
            val d = driver(); val p = d.activePlayer!!
            val producer = card("Scoped Interleaved Producer") {
                typeLine = "Snow Land"
                activatedAbility { cost = Costs.Tap
                    effect = Effects.AddDynamicMana(DynamicAmount.Fixed(2), Color.entries.toSet()) then
                        Effects.AddAnyColorMana(1) then
                        Effects.AddDynamicMana(DynamicAmount.Fixed(2), Color.entries.toSet())
                    manaAbility = true }
            }
            d.registerCards(listOf(producer))
            if (dampened) d.putPermanentOnBattlefield(p, damp.name)
            scope(d, pauseAfter = true)
            val source = d.putLandOnBattlefield(p, producer.name)
            val events = mutableListOf<GameEvent>()
            val activation = d.submit(ActivateAbility(p, source, producer.script.activatedAbilities.first().id,
                manaColorChoice = Color.GREEN))
            activation.error shouldBe null; events += activation.events
            repeat(4) { index ->
                roundTrip(d)
                events.filterIsInstance<ManaAddedEvent>() shouldBe emptyList()
                val decision = d.pendingDecision as ChooseColorDecision
                val answer = d.submitDecision(p, ColorChosenResponse(decision.id, Color.GREEN))
                answer.error shouldBe null; events += answer.events
                if (index < 3) answer.events.filterIsInstance<ManaAddedEvent>() shouldBe emptyList()
            }
            assertBatch(d, if (dampened) 1 else 5)
            events.filterIsInstance<ManaAddedEvent>().single().let {
                it.green shouldBe if (dampened) 0 else 5
                it.colorless shouldBe if (dampened) 1 else 0
            }
        }
    }
    test("dampening sees all fixed parts before tagging the one replacement unit") {
        val d = driver(); d.putPermanentOnBattlefield(d.activePlayer!!, damp.name); scope(d)
        activate(d, fixed).error shouldBe null
        assertBatch(d, 1)
        pool(d).restrictedMana.single().color shouldBe null
    }
    test("dampening sees all pip choices rather than replacing each part") {
        val d = driver(); d.putPermanentOnBattlefield(d.activePlayer!!, damp.name); scope(d, pauseAfter = true)
        activate(d, pips).error shouldBe null
        repeat(3) {
            val decision = d.pendingDecision as ChooseColorDecision
            d.submitDecision(d.activePlayer!!, ColorChosenResponse(decision.id, Color.GREEN)).error shouldBe null
        }
        assertBatch(d, 1); pool(d).restrictedMana.single().color shouldBe null
    }
    test("one unit pays the multipart activation obligation and excess keeps its snow source") {
        val d = driver(); val p = d.activePlayer!!; val card = d.putCardInHand(p, spell.name)
        scope(d); activate(d, fixed).error shouldBe null
        d.submit(CastSpell(p, card)).error shouldBe null
        d.state.activeManaSpendingScope(p)!!.pendingIds shouldBe emptySet()
        pool(d).restrictedMana.single().let {
            it.color shouldBe Color.BLUE; it.obligationIds shouldBe emptySet(); it.source?.isSnow shouldBe true
        }
    }
    test("restricted multipart output preserves its restriction expiry and one identity") {
        val d = driver(); val p = d.activePlayer!!
        val restriction = com.wingedsheep.sdk.scripting.effects.ManaRestriction.CreatureSpellsOnly
        val producer = card("Scoped Restricted Producer") {
            typeLine = "Snow Land"
            activatedAbility { cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN, 1, restriction = restriction,
                    expiry = com.wingedsheep.sdk.scripting.effects.ManaExpiry.END_OF_COMBAT) then
                    Effects.AddDynamicMana(DynamicAmount.Fixed(2), setOf(Color.RED, Color.GREEN), restriction)
                manaAbility = true }
        }
        d.registerCards(listOf(producer)); scope(d, pauseAfter = true)
        activate(d, producer).error shouldBe null; roundTrip(d)
        val decision = d.pendingDecision as ChooseNumberDecision
        d.submitDecision(p, NumberChosenResponse(decision.id, 1)).error shouldBe null
        assertBatch(d, 3)
        pool(d).restrictedMana.all { it.restriction == restriction } shouldBe true
        pool(d).restrictedMana.first().expiry shouldBe com.wingedsheep.sdk.scripting.effects.ManaExpiry.END_OF_COMBAT
    }
    test("sacrificed animated source keeps projected mana provenance across its color pause") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Sacrificed Producer") {
            typeLine = "Snow Land"
            activatedAbility { cost = Costs.SacrificeSelf
                effect = Effects.AddMana(Color.BLUE, 1,
                    restriction = com.wingedsheep.sdk.scripting.effects.ManaRestriction.CreatureSpellsOnly) then
                    Effects.AddDynamicMana(DynamicAmount.Fixed(2), setOf(Color.RED, Color.GREEN))
                manaAbility = true }
        }
        d.registerCards(listOf(producer)); val source = d.putLandOnBattlefield(p, producer.name)
        val animated = d.services.effectExecutorRegistry.execute(d.state, Effects.AnimateLand(
            com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(source), 2, 2),
            EffectContext(sourceId = null, controllerId = p))
        animated.error shouldBe null; d.replaceState(animated.state)
        d.state.projectedState.isCreature(source) shouldBe true
        scope(d, pauseAfter = true)
        d.submit(ActivateAbility(p, source, producer.script.activatedAbilities.first().id)).error shouldBe null
        roundTrip(d)
        val decision = d.pendingDecision as ChooseNumberDecision
        d.submitDecision(p, NumberChosenResponse(decision.id, 1)).error shouldBe null
        assertBatch(d, 3)
        pool(d).restrictedMana.all { CardType.CREATURE in it.source!!.cardTypes && it.source!!.sourceId == source } shouldBe true
        (source in d.state.getBattlefield()) shouldBe false
    }
    test("preexisting snow and ordinary mana are not included in the new activation batch") {
        val d = driver(); val p = d.activePlayer!!
        d.giveMana(p, Color.GREEN, 2)
        d.replaceState(d.state.updateEntity(p) { it.with(pool(d).markSnow(Color.GREEN, 1)) })
        scope(d); activate(d, fixed).error shouldBe null
        assertBatch(d, 2)
        pool(d).green shouldBe 2; pool(d).snowMana[Color.GREEN] shouldBe 1
    }
    test("a non-tapping activation on an already tapped source does not fire tap bonuses") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Scoped Nontap Producer") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Mana("{0}")
                effect = Effects.AddMana(Color.GREEN, 1) then Effects.AddMana(Color.BLUE, 1)
                manaAbility = true }
        }
        d.registerCards(listOf(producer)); d.putPermanentOnBattlefield(p, bonus.name)
        val source = d.putLandOnBattlefield(p, producer.name)
        d.replaceState(d.state.updateEntity(source) { it.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent) })
        scope(d)
        val result = d.submit(ActivateAbility(p, source, producer.script.activatedAbilities.first().id))
        result.error shouldBe null
        pool(d).red shouldBe 0; pool(d).restrictedMana.size shouldBe 2
        result.events.filterIsInstance<LandTappedForManaEvent>() shouldBe emptyList()
    }
    test("zero production creates an unsatisfied obligation and does not trigger a tap bonus") {
        val d = driver(); d.putPermanentOnBattlefield(d.activePlayer!!, bonus.name); scope(d)
        activate(d, zero).error shouldBe null
        d.state.activeManaSpendingScope(d.activePlayer!!)!!.pendingIds.size shouldBe 1
        pool(d).restrictedMana shouldBe emptyList(); pool(d).red shouldBe 0
    }
})
