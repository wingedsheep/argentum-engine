package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class EffectDiscardDestinationTest : ScenarioTestBase() {
    init {
        val redirect = card("Effect Discard Test Redirect") {
            typeLine = "Artifact"
            replacementEffect(OptionalEffectDiscardDestination(CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)))
        }
        val filtered = card("Effect Discard Test Filtered") {
            typeLine = "Artifact"
            replacementEffect(OptionalEffectDiscardDestination(CardDestination.ToZone(Zone.LIBRARY),
                EventPattern.DiscardEvent(Player.EachOpponent, GameObjectFilter.Creature)))
        }
        val madness = card("Effect Discard Test Madness") {
            typeLine = "Creature — Beast"; power = 2; toughness = 2; madness("{R}")
        }
        val selfDiscard = card("Effect Discard Test Self Trigger") {
            typeLine = "Sorcery"
            triggeredAbility {
                trigger = Triggers.self.isDiscarded()
                effect = Effects.GainLife(1)
            }
        }
        listOf(redirect, filtered, madness, selfDiscard).forEach(cardRegistry::register)
        fun board(source: String = redirect.name, sourcePlayer: Int = 1, cards: List<String> = listOf("Grizzly Bears")) = scenario()
            .withPlayers("First", "Second").withCardOnBattlefield(sourcePlayer, source)
            .apply { cards.forEach { withCardInHand(1, it) } }
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(1).withPriorityPlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun run(game: TestGame, effect: Effect = Patterns.Hand.discardHand(), controller: Int = 1, target: com.wingedsheep.sdk.model.EntityId? = null) {
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = if (controller == 1) game.player1Id else game.player2Id,
                    targets = target?.let { listOf(ChosenTarget.Permanent(it)) }.orEmpty()))
            result.error shouldBe null; game.state = result.state
        }
        fun choose(game: TestGame, index: Int = 0): ExecutionResult {
            val decision = game.state.pendingDecision as ChooseOptionDecision
            return game.submitDecision(OptionChosenResponse(decision.id, index)).also { it.error shouldBe null }
        }
        test("filter and opponent scope both apply") {
            val game = board(filtered.name, 2, listOf("Grizzly Bears", "Mountain"))
            run(game)
            val decision = game.state.pendingDecision as ChooseOptionDecision
            decision.prompt.contains("Grizzly Bears") shouldBe true
            choose(game)
            game.graveyardSize(1) shouldBe 1
            game.state.getLibrary(game.player1Id).size shouldBe 2
        }
        test("wrong player scope does not offer replacement") {
            val game = board(filtered.name)
            run(game)
            game.state.pendingDecision shouldBe null
            game.graveyardSize(1) shouldBe 1
        }
        test("projected control change moves the player-relative scope") {
            val game = board(sourcePlayer = 2)
            run(game, Effects.GainControl(EffectTarget.ContextTarget(0)), target = game.findPermanent(redirect.name)!!)
            run(game)
            (game.state.pendingDecision as ChooseOptionDecision).playerId shouldBe game.player1Id
            choose(game)
            game.graveyardSize(1) shouldBe 0
        }
        test("a grant to a stolen permanent captures its projected controller") {
            val game = board(sourcePlayer = 2)
            val source = game.findPermanent(redirect.name)!!
            run(game, Effects.GainControl(EffectTarget.ContextTarget(0)), target = source)
            run(game, Effects.GrantReplacementEffect(OptionalEffectDiscardDestination(CardDestination.ToZone(Zone.LIBRARY)),
                EffectTarget.ContextTarget(0), Duration.EndOfTurn), target = source)
            game.state.grantedReplacementEffects.single().controllerId shouldBe game.player1Id
            game.state = services.zones.moveToZone(game.state, source, Zone.GRAVEYARD).state
            run(game); choose(game)
            game.state.getLibrary(game.player1Id).size shouldBe 2
        }
        test("lost abilities suppress printed replacement") {
            val game = board()
            run(game, Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)), target = game.findPermanent(redirect.name)!!)
            run(game)
            game.state.pendingDecision shouldBe null
            game.graveyardSize(1) shouldBe 1
        }
        test("granted replacement survives its source leaving") {
            val game = board()
            val id = game.findPermanent(redirect.name)!!
            run(game, Effects.GrantReplacementEffect(OptionalEffectDiscardDestination(CardDestination.ToZone(Zone.LIBRARY)), EffectTarget.ContextTarget(0), Duration.EndOfTurn), target = id)
            game.state = services.zones.moveToZone(game.state, id, Zone.GRAVEYARD).state
            run(game)
            choose(game)
            game.state.getLibrary(game.player1Id).size shouldBe 2
        }
        test("empty discard does not prompt or emit a discard") {
            val game = board(cards = emptyList())
            run(game)
            game.state.pendingDecision shouldBe null
        }
        test("player can choose library ahead of madness") {
            val game = board(cards = listOf(madness.name))
            val id = game.state.getHand(game.player1Id).single()
            run(game); choose(game)
            game.state.getLibrary(game.player1Id).first() shouldBe id
            game.state.getExile(game.player1Id).contains(id) shouldBe false
            game.state.stack shouldBe emptyList()
        }
        test("declining keeps the madness discard replacement") {
            val game = board(cards = listOf(madness.name))
            val id = game.state.getHand(game.player1Id).single()
            run(game); choose(game, 1)
            game.state.getExile(game.player1Id).contains(id) shouldBe true
        }
        test("post-discard instruction draws only after destination choices finish") {
            val game = board()
            val id = game.state.getHand(game.player1Id).single()
            run(game, Effects.Composite(listOf(Patterns.Hand.discardHand(), Effects.DrawCards(1))))
            game.handSize(1) shouldBe 1
            choose(game)
            game.state.getHand(game.player1Id) shouldBe listOf(id)
            game.state.getLibrary(game.player1Id).size shouldBe 1
        }
        test("connive does not count an unrevealed library discard as nonland") {
            val game = board()
            // The drawn Forest and existing creature offer the discard selection.
            run(game, Effects.Connive(target = EffectTarget.ContextTarget(0)), target = game.findPermanent(redirect.name)!!)
            val bear = game.findCardsInHand(1, "Grizzly Bears").single()
            game.selectCards(listOf(bear)).error shouldBe null
            choose(game)
            val counters = game.state.getEntity(game.findPermanent(redirect.name)!!)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
            (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
        }
        test("random discard exposes the selected card only to its owner before destination choice") {
            val game = board(cards = listOf("Grizzly Bears", "Mountain"))
            run(game, Patterns.Hand.discardRandom(1))
            val prompt = game.state.pendingDecision as ChooseOptionDecision
            (prompt.prompt.contains("Grizzly Bears") || prompt.prompt.contains("Mountain")) shouldBe true
            game.handSize(1) shouldBe 2
            choose(game)
            game.handSize(1) shouldBe 1
        }
        test("a pipeline used for a ward cost skips effect discard replacements") {
            val game = board()
            val result = services.effectExecutorRegistry.execute(game.state, Patterns.Hand.discardHand(),
                EffectContext(sourceId = null, controllerId = game.player1Id, discardIsCost = true))
            result.error shouldBe null
            result.state.pendingDecision shouldBe null
            result.state.getGraveyard(game.player1Id).size shouldBe 1
        }
        test("discard-cost marker survives the card-selection pause") {
            val game = board(cards = listOf("Grizzly Bears", "Mountain"))
            val bear = game.findCardsInHand(1, "Grizzly Bears").single()
            val result = services.effectExecutorRegistry.execute(game.state, Patterns.Hand.discardCards(1),
                EffectContext(sourceId = null, controllerId = game.player1Id, discardIsCost = true))
            result.error shouldBe null; game.state = result.state
            game.selectCards(listOf(bear)).error shouldBe null
            game.state.pendingDecision shouldBe null
            game.state.getGraveyard(game.player1Id) shouldBe listOf(bear)
        }
        test("undefined characteristics do not satisfy a creature discard trigger") {
            val game = board()
            run(game)
            val result = choose(game)
            val event = result.events.filterIsInstance<CardsDiscardedEvent>().single()
            val matcher = com.wingedsheep.engine.event.TriggerMatcher(services.predicateEvaluator, services.conditionEvaluator)
            val source = game.findPermanent(redirect.name)!!
            matcher.matchingDiscardCount(EventPattern.DiscardEvent(Player.You), event, source, game.player1Id, game.state) shouldBe 1
            matcher.matchingDiscardCount(EventPattern.DiscardEvent(Player.You, GameObjectFilter.Creature), event, source, game.player1Id, game.state) shouldBe 0
            val recursiveUnion = GameObjectFilter.Creature or GameObjectFilter.Artifact.ownedByYou()
            recursiveUnion.cardPredicates.isEmpty() shouldBe true
            matcher.matchingDiscardCount(EventPattern.DiscardEvent(Player.You, recursiveUnion), event, source, game.player1Id, game.state) shouldBe 0
            val independentOwnerBranch = GameObjectFilter.Creature or GameObjectFilter.Any.ownedByYou()
            matcher.matchingDiscardCount(EventPattern.DiscardEvent(Player.You, independentOwnerBranch), event, source, game.player1Id, game.state) shouldBe 1
            val nestedNegation = GameObjectFilter(statePredicates = listOf(
                com.wingedsheep.sdk.scripting.predicates.StatePredicate.Not(
                    com.wingedsheep.sdk.scripting.predicates.StatePredicate.Or(listOf(
                        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasManaAbility,
                        com.wingedsheep.sdk.scripting.predicates.StatePredicate.InZone(Zone.GRAVEYARD))))))
            matcher.matchingDiscardCount(EventPattern.DiscardEvent(Player.You, nestedNegation), event, source, game.player1Id, game.state) shouldBe 0
        }
        test("an unrevealed hidden discard neither fires its own ability nor exposes its identity") {
            val game = board(cards = listOf(selfDiscard.name))
            val cardId = game.state.getHand(game.player1Id).single()
            run(game)
            val result = choose(game)
            game.state.getLibrary(game.player1Id).first() shouldBe cardId
            game.state.stack shouldBe emptyList()
            result.events.filterIsInstance<AbilityTriggeredEvent>() shouldBe emptyList()
            val codec = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
            val restoredEvents = codec.decodeFromString<List<GameEvent>>(codec.encodeToString(result.events))
            for (events in listOf(result.events, restoredEvents)) {
                val own = com.wingedsheep.engine.view.ClientEventTransformer.transform(events, game.player1Id, game.state)
                    .filterIsInstance<com.wingedsheep.engine.view.ClientEvent.CardDiscarded>().single()
                own.cardId shouldBe cardId
                own.cardName shouldBe selfDiscard.name
                for (viewer in listOf(game.player2Id, com.wingedsheep.sdk.model.EntityId.of("public-viewer"))) {
                    val visible = com.wingedsheep.engine.view.ClientEventTransformer.transform(events, viewer, game.state)
                    visible.any { it.description.contains(selfDiscard.name) } shouldBe false
                    visible.filterIsInstance<com.wingedsheep.engine.view.ClientEvent.CardDiscarded>().single().cardId shouldBe null
                    visible.filterIsInstance<com.wingedsheep.engine.view.ClientEvent.PermanentLeft>().single().cardId shouldBe null
                }
            }
        }
        test("a self-discard ability still fires when the card goes to the graveyard") {
            val game = board(cards = listOf(selfDiscard.name))
            run(game)
            val result = choose(game, 1)
            result.events.filterIsInstance<AbilityTriggeredEvent>().size shouldBe 1
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
        }
        test("a fresh library query reads normal characteristics after an undefined discard") {
            val game = board()
            run(game, Effects.Composite(listOf(Patterns.Hand.discardHand(),
                GatherCardsEffect(CardSource.FromZone(Zone.LIBRARY), "fresh"),
                FilterCollectionEffect("fresh", GameObjectFilter.Creature, storeMatching = "fresh_creatures"),
                FilterCollectionEffect("fresh_creatures", GameObjectFilter.Creature, storeMatching = "known_creatures"),
                Effects.GainLife(DynamicAmounts.storedNumber("known_creatures_count")))))
            choose(game)
            game.getLifeTotal(1) shouldBe 21
        }
        test("undefined discard characteristics cannot be ranked or read as a pipeline property") {
            val game = board()
            val bear = game.state.getHand(game.player1Id).single()
            val context = EffectContext(sourceId = null, controllerId = game.player1Id,
                pipeline = com.wingedsheep.engine.handlers.PipelineState(storedCollections = mapOf(
                    "discarded" to listOf(bear),
                    com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations.UNDEFINED + ":discarded" to listOf(bear))))
            val filtered = services.effectExecutorRegistry.execute(game.state,
                FilterCollectionEffect("discarded", GameObjectFilter.Any, storeMatching = "ranked", collectionFilter = CollectionFilter.GreatestManaValue), context)
            filtered.updatedCollections["ranked"] shouldBe emptyList()
            services.predicateEvaluator.amounts.evaluate(game.state,
                DynamicAmounts.powerOf(EffectTarget.PipelineTarget("discarded")), context) shouldBe 0
        }
        test("paused destination frames round trip and keep target identity") {
            val game = board()
            run(game, Effects.Composite(listOf(Patterns.Hand.discardHand(), Effects.GainLife(2))))
            val frame = (game.state.peekContinuation() as Suspension).answer as EffectDiscardDestinationContinuation
            val codec = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
            val copy = codec.decodeFromString<AnswerContinuation>(codec.encodeToString<AnswerContinuation>(frame))
            copy shouldBe frame
            game.state = codec.decodeFromString<com.wingedsheep.engine.state.GameState>(codec.encodeToString(game.state))
            choose(game)
            game.getLifeTotal(1) shouldBe 22
        }
    }
}
