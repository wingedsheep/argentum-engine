package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ManaSpendingObligationsTest : FunSpec({
    val producer = card("Obligation Repeat Producer") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Mana("{0}"); effect = Effects.AddMana(Color.GREEN, 2); manaAbility = true }
    }
    val filter = card("Obligation Filter Land") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Mana("{G}"); effect = Effects.AddMana(Color.BLUE, 1); manaAbility = true }
    }
    val choice = card("Obligation Color Choice Land") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddAnyColorMana(2); manaAbility = true }
    }
    val paid = card("Obligation Scoped Spell") {
        manaCost = "{G}"; typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(producer, filter, choice, paid))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scope(d: GameTestDriver, p: EntityId = d.activePlayer!!, id: String = "scope") {
        d.replaceState(d.state.pushContinuation(ManaSpendingObligationsContinuation(p,
            EffectContext(sourceId = null, controllerId = p), id)))
    }
    fun pool(d: GameTestDriver, p: EntityId = d.activePlayer!!) = d.state.getEntity(p)!!.get<ManaPoolComponent>()!!
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }

    test("manual fixed output records exact units and a distinct identity for each activation") {
        val d = driver(); val p = d.activePlayer!!; val source = d.putLandOnBattlefield(p, producer.name)
        scope(d)
        repeat(2) {
            d.submit(ActivateAbility(p, source, producer.script.activatedAbilities.first().id)).error shouldBe null
        }
        val ids = d.state.activeManaSpendingScope(p)!!.pendingIds
        ids.size shouldBe 2
        pool(d).green shouldBe 0
        pool(d).restrictedMana.size shouldBe 4
        pool(d).restrictedMana.flatMap { it.obligationIds }.groupingBy { it }.eachCount().values.toSet() shouldBe setOf(2)
        pool(d).restrictedMana.flatMap { it.obligationIds }.toSet() shouldBe ids
    }
    test("paying a permitted filter activation discharges prior production and tracks the new batch") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, producer.name); val converter = d.putLandOnBattlefield(p, filter.name)
        scope(d)
        d.submit(ActivateAbility(p, source, producer.script.activatedAbilities.first().id)).error shouldBe null
        val old = d.state.activeManaSpendingScope(p)!!.pendingIds.single()
        d.submit(ActivateAbility(p, converter, filter.script.activatedAbilities.first().id)).error shouldBe null
        val current = d.state.activeManaSpendingScope(p)!!.pendingIds
        current.size shouldBe 1
        (old in current) shouldBe false
        pool(d).restrictedMana.single { it.color == Color.GREEN }.obligationIds shouldBe emptySet()
        pool(d).restrictedMana.single { it.color == Color.BLUE }.obligationIds shouldBe current
    }
    test("another player's activation neither tags their pool nor creates obligations") {
        val d = driver(); val p = d.activePlayer!!; val q = d.getOpponent(p)
        val source = d.putLandOnBattlefield(q, producer.name)
        scope(d); d.replaceState(d.state.withPriority(q))
        d.submit(ActivateAbility(q, source, producer.script.activatedAbilities.first().id)).error shouldBe null
        pool(d, q).green shouldBe 2
        pool(d, q).restrictedMana shouldBe emptyList()
        d.state.activeManaSpendingScope(p)!!.pendingIds shouldBe emptySet()
    }
    test("a standalone color choice retains its production scope across serialization") {
        val d = driver(); val p = d.activePlayer!!; val source = d.putLandOnBattlefield(p, choice.name)
        scope(d)
        // Stop the color-choice resumer at another instruction, before it closes the unpaid scope.
        d.replaceState(d.state.pushContinuation(EffectContinuation(listOf(Effects.May(Effects.GainLife(1))),
            EffectContext(sourceId = null, controllerId = p))))
        d.submit(ActivateAbility(p, source, choice.script.activatedAbilities.first().id)).error shouldBe null
        val decision = d.pendingDecision as ChooseColorDecision
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitDecision(p, ColorChosenResponse(decision.id, Color.RED)).error shouldBe null
        pool(d).restrictedMana.size shouldBe 2
        val ids = d.state.activeManaSpendingScope(p)!!.pendingIds
        ids.size shouldBe 1
        pool(d).restrictedMana.all { it.color == Color.RED && it.obligationIds == ids } shouldBe true
    }
    test("a synchronous instruction without production removes its wrapper") {
        val d = driver(); val p = d.activePlayer!!
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.WithManaSpendingObligations(Effects.GainLife(1)), EffectContext(sourceId = null, controllerId = p))
        result.error shouldBe null; result.pendingDecision shouldBe null
        result.state.continuationStack shouldBe emptyList()
    }
    test("a single production is owed by every enclosing scope and one payment discharges all") {
        val d = driver(); val p = d.activePlayer!!; val source = d.putLandOnBattlefield(p, producer.name)
        scope(d, id = "outer"); scope(d, id = "inner")
        d.submit(ActivateAbility(p, source, producer.script.activatedAbilities.first().id)).error shouldBe null
        val frames = d.state.continuationStack.filterIsInstance<ManaSpendingObligationsContinuation>()
        frames.size shouldBe 2
        frames[0].pendingIds shouldBe frames[1].pendingIds
        frames[0].pendingIds.size shouldBe 1
        val before = pool(d).restrictedMana
        val after = before.drop(1)
        d.replaceState(settleManaObligationPayment(d.state.updateEntity(p) { it.with(pool(d).copy(restrictedMana = after)) }, p, before, after))
        d.state.continuationStack.filterIsInstance<ManaSpendingObligationsContinuation>().all { it.pendingIds.isEmpty() } shouldBe true
    }
    test("completed play collections cross serialized scopes and the registered resumer cleans up") {
        val d = driver(); val p = d.activePlayer!!; val actor = d.getOpponent(p)
        val chosen = d.putCardInHand(p, paid.name); d.giveMana(p, Color.GREEN, 1)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.WithManaSpendingObligations(Effects.ForcePlay("chosen", EffectTarget.ContextTarget(0), "played"), EffectTarget.ContextTarget(0)) then
                Effects.ControlPlayerDuringResolution(EffectTarget.ContextTarget(0), EffectTarget.PipelineTarget("played")),
            EffectContext(sourceId = null, controllerId = actor, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(p)),
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(chosen)))))
        result.error shouldBe null
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(result.state)))
        val decision = d.pendingDecision as PlayCardDecision
        d.submitDecision(p, PlayCardResponse(decision.id, CastSpell(p, chosen))).error shouldBe null
        d.state.continuationStack shouldBe emptyList()
        d.state.resolutionControls.single().resolvingObject.entityId shouldBe chosen
    }
    test("triggered mana bonuses do not acquire the activated ability's obligation") {
        val d = driver(); val p = d.activePlayer!!
        val bonus = card("Obligation Mana Bonus Probe") {
            typeLine = "Enchantment"
            staticAbility { ability = com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap(
                com.wingedsheep.sdk.scripting.GameObjectFilter.Land.youControl(), Color.RED) }
        }
        d.registerCards(listOf(bonus)); d.putPermanentOnBattlefield(p, bonus.name)
        val forest = d.putLandOnBattlefield(p, "Forest")
        scope(d)
        val action = d.services.legalActionEnumerator.enumerate(d.state, p)
            .map { it.action }.filterIsInstance<ActivateAbility>().first { it.sourceId == forest }
        d.submit(action).error shouldBe null
        pool(d).red shouldBe 1
        pool(d).restrictedMana.size shouldBe 1
        pool(d).restrictedMana.single().color shouldBe Color.GREEN
        pool(d).restrictedMana.single().obligationIds shouldBe d.state.activeManaSpendingScope(p)!!.pendingIds
    }
    test("a regular color-choice instruction produces mana without an activation obligation") {
        val d = driver(); val p = d.activePlayer!!
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.WithManaSpendingObligations(Effects.AddAnyColorMana(2)),
            EffectContext(sourceId = null, controllerId = p))
        result.error shouldBe null
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(result.state)))
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(p, ColorChosenResponse(decision.id, Color.BLUE)).error shouldBe null
        pool(d).blue shouldBe 2
        pool(d).restrictedMana shouldBe emptyList()
        d.state.continuationStack shouldBe emptyList()
    }
    test("SDK wrapper round trips with its affected player and nested instruction") {
        val effect: Effect = Effects.WithManaSpendingObligations(Effects.ForcePlay("chosen"), EffectTarget.ContextTarget(0))
        json.decodeFromString<Effect>(json.encodeToString(effect)) shouldBe effect
    }
})
