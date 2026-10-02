package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.mechanics.GraveyardOrdering
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class GraveyardOrderingTest : ScenarioTestBase() {
    init {
        test("positional queries exclude the reference, filter cards, and keep order after removals") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Swamp").withCardInGraveyard(1, "Island")
                .withCardInGraveyard(1, "Llanowar Elves").withCardInGraveyard(1, "Forest").build()
            val reference = game.findCardsInGraveyard(1, "Island").single()
            val context = EffectContext(controllerId = game.player1Id, sourceId = reference).withCurrentObjectReferences(game.state)
            val above = DynamicAmounts.cardsAboveInGraveyard(filter = GameObjectFilter.Creature)
            val below = DynamicAmounts.cardsBelowInGraveyard(filter = GameObjectFilter.Creature)
            services.dynamicAmountEvaluator.evaluate(game.state, above, context) shouldBe 1
            services.dynamicAmountEvaluator.evaluate(game.state, below, context) shouldBe 1
            val elf = game.findCardsInGraveyard(1, "Llanowar Elves").single()
            game.state = zones.moveToZone(game.state, elf, Zone.EXILE).state
            services.dynamicAmountEvaluator.evaluate(game.state, above, context) shouldBe 0
            services.dynamicAmountEvaluator.evaluate(game.state, below, context) shouldBe 1
        }
        test("a departed reference returns zero even after re-entering the same graveyard") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInGraveyard(1, "Island").withCardInGraveyard(1, "Grizzly Bears").build()
            val id = game.findCardsInGraveyard(1, "Island").single()
            val context = EffectContext(controllerId = game.player1Id, sourceId = id,
                objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                    captured = true, source = game.state.objectRef(id), origin = game.state.objectRef(id)))
            val below = DynamicAmounts.cardsBelowInGraveyard()
            game.state = zones.moveToZone(game.state, id, Zone.HAND).state
            services.dynamicAmountEvaluator.evaluate(game.state, below, context) shouldBe 0
            game.state = zones.moveToZone(game.state, id, Zone.GRAVEYARD).state
            services.dynamicAmountEvaluator.evaluate(game.state, below, context) shouldBe 0
            services.dynamicAmountEvaluator.evaluate(game.state, below, EffectContext(controllerId = game.player1Id, sourceId = id)) shouldBe 1
        }
        test("graveyard queries can evaluate during projection without recursing") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInGraveyard(1, "Island").withCardInGraveyard(1, "Grizzly Bears").build()
            val context = EffectContext(controllerId = game.player1Id, sourceId = game.findCardsInGraveyard(1, "Island").single())
            services.dynamicAmountEvaluator.evaluate(game.state, DynamicAmounts.cardsAboveInGraveyard(), context,
                com.wingedsheep.engine.mechanics.layers.ProjectedState(game.state, emptyMap())) shouldBe 1
        }
        test("simultaneous owner ordering preserves older cards and object identities") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInGraveyard(1, "Swamp")
                .withCardInHand(1, "Grizzly Bears").withCardInHand(1, "Island").build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val cards = game.state.getHand(game.player1Id)
            val effect = MoveCollectionEffect("cards", CardDestination.ToZone(Zone.GRAVEYARD))
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(controllerId = game.player1Id, sourceId = null, pipeline = PipelineState(storedCollections = mapOf("cards" to cards))))
            (result.outcome is Outcome.Paused) shouldBe true
            result.events.filterIsInstance<ZoneChangeEvent>().size shouldBe 0
            game.state = result.state
            val refs = cards.map { game.state.objectRef(it) }
            val question = game.state.pendingDecision as OrderObjectsDecision
            question.playerId shouldBe game.player1Id
            question.firstLabel shouldBe "TOP OF GRAVEYARD"
            game.submitDecision(OrderedResponse(question.id, cards)).error shouldBe null
            game.state.getGraveyard(game.player1Id).takeLast(2) shouldBe cards.reversed()
            cards.map { game.state.objectRef(it) } shouldBe refs
            game.state.getGraveyard(game.player1Id).first() shouldBe game.findCardsInGraveyard(1, "Swamp").single()
        }
        test("mixed owners order their own arrivals in APNAP order") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInHand(1, "Island").withCardInHand(1, "Swamp")
                .withCardInHand(2, "Mountain").withCardInHand(2, "Forest")
                .withActivePlayer(2).withPriorityPlayer(2).build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val cards = game.state.getHand(game.player1Id) + game.state.getHand(game.player2Id)
            var state = game.state
            val events = mutableListOf<GameEvent>()
            for (id in cards) { val moved = zones.moveToZone(state, id, Zone.GRAVEYARD); state = moved.state; events += moved.events }
            val result = GraveyardOrdering.finish(ExecutionResult.success(state, events))
            game.state = result.state
            val first = game.state.pendingDecision as OrderObjectsDecision
            first.playerId shouldBe game.player2Id
            game.submitDecision(OrderedResponse(first.id, first.objects.reversed())).error shouldBe null
            val second = game.state.pendingDecision as OrderObjectsDecision
            second.playerId shouldBe game.player1Id
            game.submitDecision(OrderedResponse(second.id, second.objects.reversed())).error shouldBe null
            game.state.pendingDecision shouldBe null
        }
        test("deterministic mode chooses insertion order and emits moves without questions") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInHand(1, "Island").withCardInHand(1, "Swamp").build()
            val cards = game.state.getHand(game.player1Id)
            val result = services.effectExecutorRegistry.execute(game.state,
                MoveCollectionEffect("cards", CardDestination.ToZone(Zone.GRAVEYARD)),
                EffectContext(controllerId = game.player1Id, sourceId = null, pipeline = PipelineState(storedCollections = mapOf("cards" to cards))))
            result.outcome shouldBe Outcome.Done
            result.state.getGraveyard(game.player1Id) shouldBe cards
            result.events.filterIsInstance<ZoneChangeEvent>().size shouldBe 2
        }
        test("SBA simultaneous deaths ask before subsequent orphan Aura cleanup") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves").withCardAttachedTo(1, "Holy Strength", "Grizzly Bears").build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            for (id in listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Llanowar Elves")!!)) {
                game.state = game.state.updateEntity(id) { it.with(com.wingedsheep.engine.state.components.battlefield.DamageComponent(10)) }
            }
            val checked = services.sbaChecker.checkAndApply(game.state)
            game.state = checked.state
            val decision = game.state.pendingDecision as OrderObjectsDecision
            decision.objects.size shouldBe 2
            game.submitDecision(OrderedResponse(decision.id, decision.objects)).error shouldBe null
            game.state.getGraveyard(game.player1Id).last() shouldBe game.findCardsInGraveyard(1, "Holy Strength").single()
        }
        test("collection outputs survive ordering before the next pipeline instruction") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Island").withCardInLibrary(1, "Forest").withCardInLibrary(2, "Swamp").build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val cards = game.state.getHand(game.player1Id)
            val pipeline = CompositeEffect(listOf(
                MoveCollectionEffect("cards", CardDestination.ToZone(Zone.GRAVEYARD), storeMovedAs = "moved"),
                Effects.GainLife(com.wingedsheep.sdk.scripting.values.DynamicAmount.DistinctEntitiesInCollections(listOf("moved")))
            ))
            val result = services.effectExecutorRegistry.execute(game.state, pipeline,
                EffectContext(sourceId = null, controllerId = game.player1Id,
                    pipeline = PipelineState(storedCollections = mapOf("cards" to cards))))
            game.state = result.state
            game.getLifeTotal(1) shouldBe 20
            val question = game.state.pendingDecision as OrderObjectsDecision
            game.submitDecision(OrderedResponse(question.id, question.objects)).error shouldBe null
            game.getLifeTotal(1) shouldBe 22
        }
        test("separate single-card instructions retain chronological order without a combined prompt") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInHand(1, "Island").withCardInHand(1, "Swamp").build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val ids = game.state.getHand(game.player1Id)
            val effect = CompositeEffect(listOf(
                MoveCollectionEffect("first", CardDestination.ToZone(Zone.GRAVEYARD)),
                MoveCollectionEffect("second", CardDestination.ToZone(Zone.GRAVEYARD))))
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = game.player1Id,
                    pipeline = PipelineState(storedCollections = mapOf("first" to listOf(ids[0]), "second" to listOf(ids[1])))))
            result.outcome shouldBe Outcome.Done
            result.state.getGraveyard(game.player1Id) shouldBe ids
        }
        test("one move instruction over multiple targets lets the owner order their arrivals") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves").withCardInGraveyard(1, "Swamp").build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val ids = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Llanowar Elves")!!)
            val effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = game.player1Id,
                    targets = ids.map { com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(it) }))
            (result.outcome is Outcome.Paused) shouldBe true
            result.events.filterIsInstance<ZoneChangeEvent>().size shouldBe 0
            game.state = result.state
            val question = game.state.pendingDecision as OrderObjectsDecision
            question.objects shouldBe ids
            game.submitDecision(OrderedResponse(question.id, ids)).error shouldBe null
            game.state.getGraveyard(game.player1Id).takeLast(2) shouldBe ids.reversed()
            game.state.getGraveyard(game.player1Id).first() shouldBe game.findCardsInGraveyard(1, "Swamp").single()
        }
        test("per-target composite instructions keep chronological arrivals") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves").build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val ids = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Llanowar Elves")!!)
            val effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)), Effects.GainLife(1))
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = game.player1Id,
                    targets = ids.map { com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(it) }))
            result.outcome shouldBe Outcome.Done
            result.state.getGraveyard(game.player1Id) shouldBe ids
            game.state = result.state
            game.getLifeTotal(1) shouldBe 22
        }
        test("a selected sacrifice batch orders before siblings read the graveyard and retains sacrifice snapshots") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves").withCardOnBattlefield(1, "Birds of Paradise").build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val bear = game.findPermanent("Grizzly Bears")!!
            val elf = game.findPermanent("Llanowar Elves")!!
            val effect = CompositeEffect(listOf(
                Effects.Sacrifice(GameObjectFilter.Creature, count = 2, target = EffectTarget.Controller),
                Effects.GainLife(DynamicAmounts.cardsAboveInGraveyard(EffectTarget.SpecificEntity(bear))),
                Effects.GainLife(com.wingedsheep.sdk.scripting.values.DynamicAmount.TotalPowerSacrificedThisWay)
            ))
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = game.player1Id))
            game.state = result.state
            val selection = game.state.pendingDecision as SelectCardsDecision
            val sacrificed = game.submitDecision(CardsSelectedResponse(selection.id, listOf(bear, elf)))
            sacrificed.error shouldBe null
            sacrificed.events.filterIsInstance<ZoneChangeEvent>().size shouldBe 0
            game.getLifeTotal(1) shouldBe 20
            val ordering = game.state.pendingDecision as OrderObjectsDecision
            // Bear is topmost: the positional life-gain instruction must see zero above it.
            val resumed = game.submitDecision(OrderedResponse(ordering.id, listOf(bear, elf)))
            resumed.error shouldBe null
            resumed.events.filterIsInstance<ZoneChangeEvent>().size shouldBe 2
            game.getLifeTotal(1) shouldBe 23
            game.state.getGraveyard(game.player1Id) shouldBe listOf(elf, bear)
        }
        test("replacement-diverted cards are excluded from graveyard ordering") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInHand(1, "Island").withCardInHand(1, "Swamp").build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val ids = game.state.getHand(game.player1Id)
            val grave = zones.moveToZone(game.state, ids[0], Zone.GRAVEYARD)
            val exile = zones.moveToZone(grave.state, ids[1], Zone.EXILE)
            val result = GraveyardOrdering.finish(ExecutionResult.success(exile.state, grave.events + exile.events))
            result.outcome shouldBe Outcome.Done
            result.state.getGraveyard(game.player1Id) shouldBe listOf(ids[0])
        }
        test("duplicate ordering response is rejected without consuming the question") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInHand(1, "Island").withCardInHand(1, "Swamp").build()
            val cards = game.state.getHand(game.player1Id)
            val result = services.effectExecutorRegistry.execute(game.state.copy(preserveGraveyardOrder = true),
                MoveCollectionEffect("cards", CardDestination.ToZone(Zone.GRAVEYARD)),
                EffectContext(sourceId = null, controllerId = game.player1Id,
                    pipeline = PipelineState(storedCollections = mapOf("cards" to cards))))
            game.state = result.state
            val question = game.state.pendingDecision as OrderObjectsDecision
            val invalid = game.submitDecision(OrderedResponse(question.id, listOf(cards[0], cards[0])))
            (invalid.outcome is Outcome.Rejected) shouldBe true
            game.state.pendingDecision shouldBe question
            game.submitDecision(OrderedResponse(question.id, cards)).error shouldBe null
        }
        test("order question serializes with deferred move events") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardInHand(1, "Island").withCardInHand(1, "Swamp").build()
            val cards = game.state.getHand(game.player1Id)
            var state = game.state.copy(preserveGraveyardOrder = true)
            val events = mutableListOf<GameEvent>()
            for (id in cards) { val moved = zones.moveToZone(state, id, Zone.GRAVEYARD); state = moved.state; events += moved.events }
            val suspended = GraveyardOrdering.finish(ExecutionResult.success(state, events)).state
            val json = kotlinx.serialization.json.Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            val restored = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), suspended))
            restored.pendingDecision shouldBe suspended.pendingDecision
            restored.continuationStack shouldBe suspended.continuationStack
        }
    }
}
