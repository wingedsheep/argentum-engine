package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.CopyTargetSpellExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

class SpellCopyRetargetingTest : FunSpec({
    val red = CopyExceptions(overrideColors = setOf(Color.RED))
    val blueBolt = card("Test Blue Copy Bolt") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(2, t)
        }
    }
    val noTargets = card("Test Blue Copy Life") {
        manaCost = "{U}"; typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }
    val modal = card("Test Blue Copy Modes") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            modal(chooseCount = 2) {
                mode("Damage a creature") {
                    val t = target(TargetFilter.Creature)
                    effect = Effects.DealDamage(1, t)
                }
                mode("Gain life") { effect = Effects.GainLife(3) }
            }
        }
    }
    val guards = listOf(Color.RED, Color.BLUE).map { color ->
        card("Test ${color.name} Copy Guard") {
            manaCost = "{W}"; typeLine = "Creature — Soldier"; power = 2; toughness = 4
            keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(color)))
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(blueBolt, noTargets, modal) + guards)
        it.initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    fun cast(d: GameTestDriver, name: String, targets: List<EntityId> = emptyList()): EntityId {
        val id = d.putCardInHand(d.player1, name)
        d.giveMana(d.player1, Color.BLUE, 5)
        d.giveMana(d.player1, Color.GREEN, 5)
        d.castSpell(d.player1, id, targets).error shouldBe null
        return id
    }
    fun castModal(d: GameTestDriver, target: EntityId): EntityId {
        val id = d.putCardInHand(d.player1, modal.name)
        d.giveMana(d.player1, Color.BLUE)
        val targets = listOf(ChosenTarget.Permanent(target))
        d.submit(CastSpell(playerId = d.player1, cardId = id, targets = targets,
            paymentStrategy = PaymentStrategy.FromPool, chosenModes = listOf(0, 1),
            modeTargetsOrdered = listOf(targets, emptyList()))).error shouldBe null
        return id
    }
    fun copy(d: GameTestDriver, source: EntityId, exceptions: CopyExceptions = red, count: Int = 1): EffectResult {
        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val result = CopyTargetSpellExecutor(predicates.conditions.amounts, TargetFinder(predicates)).execute(
            d.state, CopyTargetSpellEffect(EffectTarget.ContextTarget(0), copies = DynamicAmount.Fixed(count), exceptions = exceptions),
            EffectContext(sourceId = null, controllerId = d.player2, targets = listOf(ChosenTarget.Spell(source)))
        )
        result.error shouldBe null
        d.replaceState(result.state)
        return result
    }
    fun copies(d: GameTestDriver) = d.state.stack.filter { d.state.getEntity(it)?.has<CopyOfComponent>() == true }
    fun roundTrip(d: GameTestDriver) {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        d.replaceState(json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), d.state)))
    }
    test("an illegal inherited target can be kept while another legal target exists") {
        val d = driver()
        val original = d.putPermanentOnBattlefield(d.player1, guards[0].name)
        d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = cast(d, blueBolt.name, listOf(original))
        copy(d, source)
        d.submitTargetSelection(d.player2, emptyList()).error shouldBe null
        d.state.getEntity(copies(d).single())!!.get<TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Permanent(original))
        d.bothPass().error shouldBe null
        d.state.getEntity(original)!!.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>() shouldBe null
    }
    test("forged protected replacements fail without losing the pending decision") {
        val d = driver()
        val original = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val protected = d.putPermanentOnBattlefield(d.player1, guards[0].name)
        val source = cast(d, blueBolt.name, listOf(original))
        copy(d, source)
        val before = d.state
        (d.submitTargetSelection(d.player2, listOf(protected)).error != null) shouldBe true
        d.state shouldBe before
        d.submitTargetSelection(d.player2, emptyList()).error shouldBe null
    }
    test("multiple slots can keep one illegal target and change another with the division fixed") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, guards[0].name)
        val b = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val c = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        val source = cast(d, blueBolt.name, listOf(a))
        val req = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = TargetFilter.Creature, count = 2)
        val spell = d.state.getEntity(source)!!.get<SpellOnStackComponent>()!!
        d.replaceState(d.state.updateEntity(source) { it
            .with(TargetsComponent.capture(d.state, listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b)), listOf(req)))
            .with(spell.copy(damageDistribution = mapOf(a to 1, b to 3))) })
        copy(d, source)
        val q = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        q.targetRequirements.size shouldBe 2
        q.targetRequirements.map { it.maxTargets } shouldBe listOf(1, 1)
        roundTrip(d)
        d.submit(SubmitDecision(d.player2, TargetsResponse(q.id, mapOf(1 to listOf(c), 0 to emptyList())))).error shouldBe null
        val copied = d.state.getEntity(copies(d).single())!!
        copied.get<TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(c))
        copied.get<SpellOnStackComponent>()!!.damageDistribution shouldBe mapOf(a to 1, c to 3)
        d.state.getEntity(source)!!.get<SpellOnStackComponent>()!!.damageDistribution shouldBe mapOf(a to 1, b to 3)
    }
    test("duplicate final targets in one requirement are rejected atomically") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        val source = cast(d, blueBolt.name, listOf(a))
        val req = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = TargetFilter.Creature, count = 2)
        d.replaceState(d.state.updateEntity(source) { it.with(TargetsComponent.capture(d.state,
            listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b)), listOf(req))) })
        copy(d, source)
        val q = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val before = d.state
        (d.submit(SubmitDecision(d.player2, TargetsResponse(q.id, mapOf(0 to listOf(b))))).error != null) shouldBe true
        d.state shouldBe before
        d.submit(SubmitDecision(d.player2, TargetsResponse(q.id, mapOf(0 to listOf(b), 1 to listOf(a))))).error shouldBe null
        d.state.getEntity(copies(d).single())!!.get<TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Permanent(b), ChosenTarget.Permanent(a))
    }
    test("modal repeated copies retain omitted slots independently across serialization") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = castModal(d, a)
        copy(d, source, count = 2)
        repeat(2) {
            roundTrip(d)
            d.submitTargetSelection(d.player2, emptyList()).error shouldBe null
        }
        copies(d).size shouldBe 2
        copies(d).forEach {
            d.state.getEntity(it)!!.get<SpellOnStackComponent>()!!.modeTargetsOrdered shouldBe
                listOf(listOf(ChosenTarget.Permanent(a)), emptyList())
        }
    }
    test("a departed inherited permanent remains a permanent target rather than becoming a player") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        val source = cast(d, blueBolt.name, listOf(a))
        d.replaceState(d.state.removeEntity(a).copy(zones = d.state.zones.mapValues { (_, ids) -> ids - a }))
        copy(d, source)
        d.submitTargetSelection(d.player2, emptyList()).error shouldBe null
        d.state.getEntity(copies(d).single())!!.get<TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Permanent(a))
    }
    test("inherited object stamps survive copying but an explicitly reselected object is captured afresh") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = cast(d, blueBolt.name, listOf(a))
        val before = d.state.getEntity(source)!!.get<TargetsComponent>()!!
        val oldStamp = before.targetEntryStamps.getValue(a)
        d.replaceState(d.state.updateEntity(a) { it.with(
            com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent(oldStamp + 100)) })
        copy(d, source)
        d.submitTargetSelection(d.player2, emptyList()).error shouldBe null
        val inherited = copies(d).single()
        d.state.getEntity(inherited)!!.get<TargetsComponent>()!!.targetEntryStamps[a] shouldBe oldStamp
        copy(d, source)
        d.submitTargetSelection(d.player2, listOf(a)).error shouldBe null
        d.state.getEntity(copies(d).last())!!.get<TargetsComponent>()!!.targetEntryStamps[a] shouldBe oldStamp + 100
    }
    test("copy-each explicit reselection captures the current battlefield visit") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = cast(d, blueBolt.name, listOf(a))
        val oldStamp = d.state.getEntity(source)!!.get<TargetsComponent>()!!.targetEntryStamps.getValue(a)
        d.replaceState(d.state.updateEntity(a) { it.with(
            com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent(oldStamp + 100)) })
        val result = com.wingedsheep.engine.handlers.effects.stack.CopyEachTargetSpellExecutor(
            TargetFinder(PredicateEvaluator(cardRegistry = d.cardRegistry))
        ).execute(d.state, CopyEachTargetSpellEffect(),
            EffectContext(sourceId = null, controllerId = d.player2, targets = listOf(ChosenTarget.Spell(source))))
        result.error shouldBe null
        d.replaceState(result.state)
        d.submitTargetSelection(d.player2, listOf(a)).error shouldBe null
        d.state.getEntity(copies(d).single())!!.get<TargetsComponent>()!!.targetEntryStamps[a] shouldBe oldStamp + 100
        d.bothPass().error shouldBe null
        (a in d.state.getBattlefield()) shouldBe false
    }
    test("an optional group keeps the number actually chosen rather than its printed maximum") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = cast(d, blueBolt.name, listOf(a))
        val req = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = TargetFilter.Creature, count = 3, minCount = 0)
        d.replaceState(d.state.updateEntity(source) { it.with(TargetsComponent.capture(d.state,
            listOf(ChosenTarget.Permanent(a)), listOf(req))) })
        copy(d, source)
        val q = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        q.targetRequirements.size shouldBe 1
        val before = d.state
        (d.submit(SubmitDecision(d.player2, TargetsResponse(q.id, mapOf(0 to listOf(a, a))))).error != null) shouldBe true
        d.state shouldBe before
        d.submitTargetSelection(d.player2, emptyList()).error shouldBe null
    }
    test("an unbounded group retains all original target slots") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        val source = cast(d, blueBolt.name, listOf(a))
        val req = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = TargetFilter.Creature, unlimited = true)
        d.replaceState(d.state.updateEntity(source) { it.with(TargetsComponent.capture(d.state,
            listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b)), listOf(req))) })
        copy(d, source)
        d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().targetRequirements.size shouldBe 2
        d.submitTargetSelection(d.player2, emptyList()).error shouldBe null
        d.state.getEntity(copies(d).single())!!.get<TargetsComponent>()!!.targets.size shouldBe 2
    }
    test("distinct target words may select the same object and response key order cannot reorder slots") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        val source = cast(d, blueBolt.name, listOf(a))
        val req = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = TargetFilter.Creature)
        d.replaceState(d.state.updateEntity(source) { it.with(TargetsComponent.capture(d.state,
            listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b)), listOf(req, req))) })
        copy(d, source)
        val q = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submit(SubmitDecision(d.player2, TargetsResponse(q.id, mapOf(1 to listOf(a), 0 to listOf(a))))).error shouldBe null
        d.state.getEntity(copies(d).single())!!.get<TargetsComponent>()!!.targets shouldBe
            listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(a))
    }
    test("no legal replacements in one group does not suppress another group's choice") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = cast(d, blueBolt.name, listOf(a))
        val req = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = TargetFilter.Creature)
        d.replaceState(d.state.updateEntity(source) { it.with(TargetsComponent.capture(d.state,
            listOf(ChosenTarget.Permanent(a), ChosenTarget.Player(d.player1)),
            listOf(req, com.wingedsheep.sdk.scripting.targets.TargetPlayer()))) })
        d.replaceState(d.state.removeEntity(a).copy(zones = d.state.zones.mapValues { (_, ids) -> ids - a }))
        copy(d, source)
        val q = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        q.legalTargets.getValue(0) shouldBe emptyList()
        d.submit(SubmitDecision(d.player2, TargetsResponse(q.id, mapOf(1 to listOf(d.player2))))).error shouldBe null
        d.state.getEntity(copies(d).single())!!.get<TargetsComponent>()!!.targets shouldBe
            listOf(ChosenTarget.Permanent(a), ChosenTarget.Player(d.player2))
    }


})
