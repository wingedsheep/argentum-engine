package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** One permanent leaves, two front-face cards arrive and can be followed (CR 712.21). */
class MeldZoneTransitionTest : FunSpec({
    val services = EngineServices(CardRegistry())
    val owner = EntityId.generate()
    val opponent = EntityId.generate()
    val host = EntityId.generate()
    val partner = EntityId.generate()
    val source = EntityId.generate()
    val hostCard = CardComponent("Host", "Host", ManaCost.parse("{1}{G}"), TypeLine.parse("Creature — Human"),
        ownerId = owner, baseStats = CreatureStats(2, 2))
    val partnerCard = CardComponent("Partner", "Partner", ManaCost.parse(""), TypeLine.parse("Land"), ownerId = owner)
    val resultCard = hostCard.copy(cardDefinitionId = "Result", name = "Result", typeLine = TypeLine.parse("Creature — Avatar"),
        baseStats = CreatureStats(7, 7))

    fun board(): GameState = GameState(turnOrder = listOf(owner, opponent))
        .withEntity(owner, ComponentContainer())
        .withEntity(opponent, ComponentContainer())
        .withEntity(host, ComponentContainer().with(resultCard).with(OwnerComponent(owner))
            .with(ControllerComponent(opponent)).with(MeldedComponent(partner, hostCard)))
        .withEntity(partner, ComponentContainer().with(partnerCard).with(OwnerComponent(owner)))
        .withEntity(source, ComponentContainer().with(hostCard.copy(name = "Source")).with(OwnerComponent(owner))
            .with(ControllerComponent(owner)))
        .addToZone(ZoneKey(opponent, Zone.BATTLEFIELD), host)
        .addToZone(ZoneKey(owner, Zone.BATTLEFIELD), source)

    fun context(state: GameState) = EffectContext(sourceId = source, controllerId = owner,
        targets = listOf(ChosenTarget.Permanent(host)),
        objectReferences = ObjectReferenceEnvironment(captured = true, source = state.objectRef(source)))

    fun GameState.inZone(zone: Zone) = getZone(ZoneKey(owner, zone))
    fun execute(state: GameState, effect: Effect) = services.effectExecutorRegistry.execute(state, effect, context(state))

    test("death emits one permanent departure and two card arrivals with shared origin") {
        val initial = board()
        val moved = services.zones.moveToZone(initial, host, Zone.GRAVEYARD)
        val events = moved.events.filterIsInstance<ZoneChangeEvent>()
        events.map { it.entityId }.shouldContainExactly(host, partner)
        events.count { it.isBattlefieldDeparture } shouldBe 1
        events.map { it.oldObject }.distinct() shouldBe listOf(initial.objectRef(host))
        events.last().meldedPermanent shouldBe initial.objectRef(host)
        events.last().lastKnown shouldBe events.first().lastKnown
        moved.transitions.map { it.newObject?.entityId }.shouldContainExactly(host, partner)
        moved.state.inZone(Zone.GRAVEYARD).shouldContainExactly(host, partner)
        moved.state.getEntity(host)?.get<CardComponent>()?.name shouldBe "Host"
        moved.state.getEntity(partner)?.get<CardComponent>()?.typeLine?.isLand shouldBe true
    }

    test("death counts one permanent for controller and two cards for owner with front-face types") {
        val moved = services.zones.moveToZone(board(), host, Zone.GRAVEYARD).state
        val controller = moved.getEntity(opponent)!!
        controller.get<CreaturesDiedThisTurnComponent>()?.count shouldBe 1
        controller.get<PermanentLeftBattlefieldThisTurnComponent>()?.count shouldBe 1
        controller.get<PermanentsPutIntoGraveyardFromBattlefieldThisTurnComponent>()?.count shouldBe 1
        val player = moved.getEntity(owner)!!
        player.get<PlayerDescendedThisTurnComponent>()?.count shouldBe 2
        player.get<CreatureCardsPutIntoGraveyardThisTurnComponent>()?.count shouldBe 1
        player.get<CardsPutIntoGraveyardThisTurnComponent>()?.cardIds.shouldContainExactly(partner, host)
        for (id in listOf(host, partner)) {
            moved.getEntity(id)?.get<PutIntoGraveyardThisTurnComponent>()?.fromBattlefield shouldBe true
        }
    }

    test("exile tracks two cards but one opponent-controlled creature and stamps both origins") {
        val moved = services.zones.moveToZone(board(), host, Zone.EXILE).state
        moved.getEntity(owner)?.get<CardsPutIntoExileThisTurnComponent>()?.count shouldBe 2
        moved.getEntity(owner)?.get<OpponentCreaturesExiledThisTurnComponent>()?.count shouldBe 1
        for (id in listOf(host, partner)) moved.getEntity(id)?.get<ExiledFromZoneComponent>()?.zone shouldBe Zone.BATTLEFIELD
    }

    test("bounce counts one permanent and puts both front faces in owner's hand") {
        val moved = services.zones.moveToZone(board(), host, Zone.HAND).state
        moved.inZone(Zone.HAND).shouldContainExactly(host, partner)
        moved.getEntity(owner)?.get<PermanentsPutIntoHandFromBattlefieldThisTurnComponent>()?.count shouldBe 1
    }

    test("ordinary flicker returns both cards under the spell controller") {
        val moved = execute(board(), Effects.Exile(EffectTarget.ContextTarget(0)) then
            Effects.PutOntoBattlefieldUnderYourControl(EffectTarget.ContextTarget(0)))
        moved.outcome shouldBe Outcome.Done
        moved.state.inZone(Zone.BATTLEFIELD).shouldContainAll(host, partner)
        moved.state.inZone(Zone.EXILE).shouldBeEmpty()
        moved.state.getEntity(partner)?.get<ControllerComponent>()?.playerId shouldBe owner
        moved.events.filterIsInstance<ZoneChangeEvent>().count { it.toZone == Zone.BATTLEFIELD } shouldBe 2
    }

    test("collection flicker follows both cards even when the exile has no storeMovedAs") {
        val initial = board()
        val effect = MoveCollectionEffect("chosen", CardDestination.ToZone(Zone.EXILE)) then
            MoveCollectionEffect("chosen", CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        val moved = services.effectExecutorRegistry.execute(initial, effect,
            context(initial).copy(pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(host)))))
        moved.outcome shouldBe Outcome.Done
        moved.state.inZone(Zone.BATTLEFIELD).shouldContainAll(host, partner)
    }

    test("collection exile records and links both cards") {
        val initial = board()
        val moved = services.effectExecutorRegistry.execute(initial,
            MoveCollectionEffect("chosen", CardDestination.ToZone(Zone.EXILE), storeMovedAs = "moved", linkToSource = true, addCounterType = CounterType.TIME),
            context(initial).copy(pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(host)))))
        moved.updatedCollections["moved"].shouldContainExactly(host, partner)
        for (id in listOf(host, partner)) moved.state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.TIME) shouldBe 1
        moved.state.getEntity(source)?.get<LinkedExileComponent>()?.exiledIds.shouldContainExactly(host, partner)
    }

    test("source-departure duration records and returns both cards") {
        val exiled = execute(board(), Effects.MoveUntilSourceLeaves(EffectTarget.ContextTarget(0), Zone.EXILE))
        exiled.state.zoneReturns.map { it.movedObject.entityId }.shouldContainExactly(host, partner)
        exiled.state.getEntity(source)?.get<LinkedExileComponent>()?.exiledIds.shouldContainExactly(host, partner)
        val returned = services.zones.moveToZone(exiled.state, source, Zone.GRAVEYARD)
        returned.state.inZone(Zone.BATTLEFIELD).shouldContainExactlyInAnyOrder(host, partner)
        returned.state.zoneReturns.shouldBeEmpty()
        returned.events.filterIsInstance<ZoneChangeEvent>().count { it.transitionCause == ZoneTransitionCause.DURATION_RETURN } shouldBe 2
    }

    test("source-departure duration cannot return a partner's later exile visit") {
        val exiled = execute(board(), Effects.MoveUntilSourceLeaves(EffectTarget.ContextTarget(0), Zone.EXILE)).state
        val escaped = services.zones.moveToZone(exiled, partner, Zone.HAND).state
        val reexiled = services.zones.moveToZone(escaped, partner, Zone.EXILE).state
        val returned = services.zones.moveToZone(reexiled, source, Zone.GRAVEYARD).state
        returned.inZone(Zone.BATTLEFIELD).shouldContainExactly(host)
        returned.inZone(Zone.EXILE).shouldContainExactly(partner)
    }

    test("legacy linked exile returns both front faces") {
        val exiled = execute(board(), ExileUntilLeavesEffect(EffectTarget.ContextTarget(0)))
        exiled.state.getEntity(source)?.get<LinkedExileComponent>()?.exiledIds.shouldContainExactly(host, partner)
        val returned = execute(exiled.state, Effects.ReturnLinkedExileUnderOwnersControl())
        returned.outcome shouldBe Outcome.Done
        returned.state.inZone(Zone.BATTLEFIELD).shouldContainAll(host, partner)
    }

    test("serialized movement authorization follows surviving partner without reviving a stale host") {
        val initial = board()
        val exiled = services.zones.moveToZone(initial, host, Zone.EXILE)
        val references = context(initial).objectReferences.authorize(exiled.events)
        val decoded = Json.decodeFromString<ObjectReferenceEnvironment>(Json.encodeToString(references))
        decoded.meldedCards(host, exiled.state).shouldContainExactly(host, partner)
        val escaped = services.zones.moveToZone(exiled.state, host, Zone.HAND).state
        val reexiled = services.zones.moveToZone(escaped, host, Zone.EXILE).state
        decoded.meldedCards(host, reexiled).shouldContainExactly(partner)
        val returned = services.effectExecutorRegistry.execute(reexiled,
            Effects.PutOntoBattlefieldUnderYourControl(EffectTarget.ContextTarget(0)),
            context(initial).copy(objectReferences = decoded))
        returned.state.inZone(Zone.BATTLEFIELD).shouldContainAll(partner)
        returned.state.inZone(Zone.EXILE).shouldContainExactly(host)
    }
    test("replacement redirects both cards even when the partner does not match its creature filter") {
        val initial = board().updateEntity(source) { it.with(ReplacementEffectSourceComponent(listOf(
            com.wingedsheep.sdk.scripting.RedirectZoneChange(newDestination = Zone.EXILE,
                appliesTo = com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent(
                    filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Creature,
                    from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD))
        ))) }
        val moved = services.zones.moveToZone(initial, host, Zone.GRAVEYARD)
        moved.state.inZone(Zone.EXILE).shouldContainExactly(host, partner)
        moved.state.inZone(Zone.GRAVEYARD).shouldBeEmpty()
        moved.events.filterIsInstance<ZoneChangeEvent>().map { it.requestedDestination }.distinct() shouldBe listOf(Zone.GRAVEYARD)
        moved.transitions.map { it.requestedDestination }.distinct() shouldBe listOf(Zone.GRAVEYARD)
    }

    test("death matching counts one permanent while anywhere arrival matching sees the land front") {
        val moved = services.zones.moveToZone(board(), host, Zone.GRAVEYARD)
        val matcher = com.wingedsheep.engine.event.TriggerMatcher(services.predicateEvaluator, services.predicateEvaluator.conditions)
        val death = com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent(
            filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Creature, from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD)
        val landArrival = com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent(
            filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Land, to = Zone.GRAVEYARD)
        val events = moved.events.filterIsInstance<ZoneChangeEvent>()
        fun matching(pattern: com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent) = events.count {
            matcher.matchesZoneChangeTrigger(pattern, com.wingedsheep.sdk.scripting.TriggerBinding.ANY, it, source, owner, moved.state)
        }
        matching(death) shouldBe 1
        matching(landArrival) shouldBe 1
        val decoded = Json.decodeFromString<ZoneChangeEvent>(Json.encodeToString(events.last()))
        decoded shouldBe events.last()
        decoded.isBattlefieldDeparture shouldBe false
    }

    test("graveyard ordering offers both arriving cards in one choice") {
        val initial = board().copy(preserveGraveyardOrder = true)
        val moved = execute(initial, Effects.Destroy(EffectTarget.ContextTarget(0)))
        (moved.outcome is Outcome.Paused) shouldBe true
        val decision = moved.state.pendingDecision as OrderObjectsDecision
        decision.objects.shouldContainExactlyInAnyOrder(host, partner)
    }

    test("a death trigger returning its source finds both front-face cards") {
        val deathDefinition = com.wingedsheep.sdk.dsl.card("Returning Meld Result") {
            manaCost = ""
            typeLine = "Creature — Avatar"
            power = 7
            toughness = 7
            triggeredAbility {
                trigger = com.wingedsheep.sdk.dsl.Triggers.self.dies()
                effect = Effects.PutOntoBattlefield(EffectTarget.Self)
            }
        }
        services.cardRegistry.register(deathDefinition)
        val initial = board().updateEntity(host) {
            it.with(resultCard.copy(name = deathDefinition.name, cardDefinitionId = deathDefinition.name))
        }
        val moved = services.zones.moveToZone(initial, host, Zone.GRAVEYARD)
        val trigger = services.triggerDetector.detectTriggers(moved.state, moved.events).single()
        val returned = services.effectExecutorRegistry.execute(moved.state, trigger.ability.effect,
            EffectContext(sourceId = host, controllerId = owner, objectReferences = trigger.objectReferences))
        returned.outcome shouldBe Outcome.Done
        returned.state.inZone(Zone.BATTLEFIELD).shouldContainAll(host, partner)
        returned.state.inZone(Zone.GRAVEYARD).shouldBeEmpty()
    }

})
