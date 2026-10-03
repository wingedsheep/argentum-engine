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
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ManaAbilitySourcesTest : FunSpec({
    val paid = card("Scoped Paid Probe") {
        manaCost = "{G}"; typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }
    val rock = card("Scoped Rock Probe") {
        manaCost = "{0}"; typeLine = "Artifact"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
    }
    val landRock = card("Scoped Projected Land Probe") {
        manaCost = "{0}"; typeLine = "Artifact"
        staticAbility { ability = GrantCardType("LAND", GroupFilter.source()) }
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
    }
    val sacRock = card("Scoped Sacrifice Probe") {
        manaCost = "{0}"; typeLine = "Artifact"
        activatedAbility { cost = Costs.SacrificeSelf; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(paid, rock, landRock, sacRock))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scope(d: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId, filter: GameObjectFilter): GameState =
        d.state.pushContinuation(ManaAbilitySourcesContinuation(player, filter,
            EffectContext(sourceId = null, controllerId = d.getOpponent(player))))
    val lands = GameObjectFilter.Land.youControl()
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }

    test("automatic payment and cached affordability exclude nonland sources") {
        val d = driver(); val p = d.activePlayer!!
        val id = d.putPermanentOnBattlefield(p, rock.name)
        val cached = d.services.manaSolver.findAvailableManaSources(d.state, p)
        val restricted = scope(d, p, lands)
        d.services.manaSolver.solve(restricted, p, ManaCost.parse("{G}"), precomputedSources = cached) shouldBe null
        d.services.manaSolver.canPay(restricted, p, ManaCost.parse("{G}"), precomputedSources = cached) shouldBe false
        d.services.manaSolver.getAvailableManaCount(restricted, p, precomputedSources = cached) shouldBe 0
        d.services.manaSolver.findAvailableManaSources(restricted, p).any { it.entityId == id } shouldBe false
    }
    test("sacrifice affordability cannot count an excluded source") {
        val d = driver(); val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, sacRock.name)
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{G}")) shouldBe true
        d.services.manaSolver.canPay(scope(d, p, lands), p, ManaCost.parse("{G}")) shouldBe false
    }
    test("manual activation and action enumeration share the source restriction") {
        val d = driver(); val p = d.activePlayer!!
        val id = d.putPermanentOnBattlefield(p, rock.name)
        d.replaceState(scope(d, p, lands))
        d.services.legalActionEnumerator.enumerate(d.state, p).any { (it.action as? ActivateAbility)?.sourceId == id } shouldBe false
        val before = d.state
        d.submit(ActivateAbility(p, id, rock.script.activatedAbilities.first().id)).error.isNullOrEmpty() shouldBe false
        d.state shouldBe before
    }
    test("projected land characteristics admit a printed artifact") {
        val d = driver(); val p = d.activePlayer!!
        val id = d.putPermanentOnBattlefield(p, landRock.name)
        d.services.manaSolver.findAvailableManaSources(scope(d, p, lands), p).any { it.entityId == id } shouldBe true
    }
    test("existing floating mana remains usable and another player remains unrestricted") {
        val d = driver(); val p = d.activePlayer!!; val q = d.getOpponent(p)
        d.putPermanentOnBattlefield(q, rock.name); d.giveMana(p, Color.GREEN, 1)
        val restricted = scope(d, p, lands)
        d.services.manaSolver.canPay(restricted, p, ManaCost.parse("{G}")) shouldBe true
        d.services.manaSolver.canPay(restricted, q, ManaCost.parse("{G}")) shouldBe true
    }
    test("nested source restrictions intersect") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest")
        val restricted = scope(d, p, lands).pushContinuation(ManaAbilitySourcesContinuation(p,
            GameObjectFilter.Land.named("Island"), EffectContext(sourceId = null, controllerId = p)))
        d.services.manaSolver.canPay(restricted, p, ManaCost.parse("{G}")) shouldBe false
    }
    test("unaffordable forced play completes synchronously without leaking the scope") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, paid.name)
        d.putPermanentOnBattlefield(p, rock.name)
        val before = d.state
        val result = d.services.effectExecutorRegistry.execute(before,
            Effects.WithManaAbilitySources(Effects.ForcePlay("chosen"), lands),
            EffectContext(sourceId = null, controllerId = p,
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        result.error shouldBe null; result.pendingDecision shouldBe null
        result.state shouldBe before
    }
    test("serialized forced play retains restrictions and removes them after payment") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, paid.name)
        val forest = d.putLandOnBattlefield(p, "Forest"); val artifact = d.putPermanentOnBattlefield(p, rock.name)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.WithManaAbilitySources(Effects.ForcePlay("chosen"), lands),
            EffectContext(sourceId = null, controllerId = p,
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        result.pendingDecision shouldBe result.state.pendingDecision
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(result.state)))
        d.services.manaSolver.findAvailableManaSources(d.state, p).map { it.entityId } shouldBe listOf(forest)
        val choice = d.pendingDecision as PlayCardDecision
        d.submitDecision(p, PlayCardResponse(choice.id, CastSpell(p, id))).error shouldBe null
        d.state.continuationStack shouldBe emptyList()
        d.services.manaSolver.findAvailableManaSources(d.state, p).any { it.entityId == artifact } shouldBe true
    }
    test("source-relative filters retain the enclosing source") {
        val d = driver(); val p = d.activePlayer!!
        val allowed = d.putLandOnBattlefield(p, "Forest")
        val other = d.putLandOnBattlefield(p, "Forest")
        val state = d.state.pushContinuation(ManaAbilitySourcesContinuation(p,
            GameObjectFilter.Any.sourceItself(), EffectContext(sourceId = allowed, controllerId = d.getOpponent(p))))
        state.manaAbilitySourceAllowed(p, allowed, d.services.predicateEvaluator) shouldBe true
        state.manaAbilitySourceAllowed(p, other, d.services.predicateEvaluator) shouldBe false
    }
    test("a land's sacrifice mana ability remains legal") {
        val d = driver(); val p = d.activePlayer!!
        val land = card("Scoped Sacrificed Land") {
            typeLine = "Land"
            activatedAbility { cost = Costs.SacrificeSelf; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
        }
        d.registerCards(listOf(land))
        val id = d.putLandOnBattlefield(p, land.name)
        d.replaceState(scope(d, p, lands))
        d.submit(ActivateAbility(p, id, land.script.activatedAbilities.first().id)).error shouldBe null
        (id in d.state.getGraveyard(p)) shouldBe true
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.green shouldBe 1
    }
    test("cached snow sources cannot bypass source filters") {
        val d = driver(); val p = d.activePlayer!!
        val snowRock = card("Scoped Snow Rock") {
            manaCost = "{0}"; typeLine = "Snow Artifact"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
        }
        d.registerCards(listOf(snowRock)); d.putPermanentOnBattlefield(p, snowRock.name)
        val cached = d.services.manaSolver.findAvailableManaSources(d.state, p)
        d.services.manaSolver.solve(d.state, p, ManaCost.parse("{S}"), precomputedSources = cached)!!.sources.size shouldBe 1
        d.services.manaSolver.solve(scope(d, p, lands), p, ManaCost.parse("{S}"), precomputedSources = cached) shouldBe null
    }
    test("a paused nested scope is removed before the next sibling instruction") {
        val d = driver(); val p = d.activePlayer!!
        val artifact = d.putPermanentOnBattlefield(p, rock.name)
        val first = d.putCardInHand(p, paid.name); val second = d.putCardInHand(p, paid.name)
        d.putLandOnBattlefield(p, "Forest")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.WithManaAbilitySources(Effects.ForcePlay("first"), lands) then Effects.ForcePlay("second"),
            EffectContext(sourceId = null, controllerId = p, pipeline = PipelineState(
                storedCollections = mapOf("first" to listOf(first), "second" to listOf(second)))))
        d.replaceState(result.state)
        val choice = d.pendingDecision as PlayCardDecision
        d.submitDecision(p, PlayCardResponse(choice.id, CastSpell(p, first))).error shouldBe null
        (d.pendingDecision as PlayCardDecision).cardId shouldBe second
        d.state.continuationStack.any { it is ManaAbilitySourcesContinuation } shouldBe false
        d.services.manaSolver.findAvailableManaSources(d.state, p).any { it.entityId == artifact } shouldBe true
        val next = d.pendingDecision as PlayCardDecision
        d.submitDecision(p, PlayCardResponse(next.id, CastSpell(p, second))).error shouldBe null
        d.state.continuationStack shouldBe emptyList()
    }
    test("completed-play collections pass through the scope to an outer continuation") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, paid.name)
        d.putLandOnBattlefield(p, "Forest")
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.WithManaAbilitySources(Effects.ForcePlay("chosen", EffectTarget.ContextTarget(0), "played"), lands, EffectTarget.ContextTarget(0)) then
                Effects.ControlPlayerDuringResolution(EffectTarget.ContextTarget(0), EffectTarget.PipelineTarget("played")),
            EffectContext(sourceId = null, controllerId = d.getOpponent(p),
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(p)),
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        d.replaceState(result.state)
        val choice = d.pendingDecision as PlayCardDecision
        d.submitDecision(p, PlayCardResponse(choice.id, CastSpell(p, id))).error shouldBe null
        d.state.resolutionControls.single().playerId shouldBe p
        d.state.resolutionControls.single().resolvingObject.entityId shouldBe id
    }
    test("pipeline metadata passes through nested scopes without changing their captured context") {
        val d = driver(); val p = d.activePlayer!!
        val context = EffectContext(sourceId = null, controllerId = p)
        val outer = EffectContinuation(listOf(Effects.GainLife(1)), context)
        val frame = ManaAbilitySourcesContinuation(p, lands, context)
        val before = d.state.pushContinuation(outer).pushContinuation(frame).pushContinuation(frame)
        val after = com.wingedsheep.engine.handlers.continuations.exposeCollectionsToNextFrame(before,
            collections = mapOf("cards" to d.state.getHand(p)), numbers = mapOf("count" to 3),
            chosenValues = mapOf("choice" to "green"), subtypeGroups = mapOf("types" to listOf(setOf("Elf"))))
        after.continuationStack.takeLast(2) shouldBe listOf(frame, frame)
        val pipeline = (after.continuationStack.first() as EffectContinuation).effectContext.pipeline
        pipeline.storedCollections["cards"] shouldBe d.state.getHand(p)
        pipeline.storedNumbers["count"] shouldBe 3
        pipeline.chosenValues["choice"] shouldBe "green"
        pipeline.storedSubtypeGroups["types"] shouldBe listOf(setOf("Elf"))
    }
    test("SDK scope and nested effect round trip") {
        val effect: Effect = Effects.WithManaAbilitySources(Effects.ForcePlay("chosen"), lands)
        json.decodeFromString<Effect>(json.encodeToString(effect)) shouldBe effect
    }
})
