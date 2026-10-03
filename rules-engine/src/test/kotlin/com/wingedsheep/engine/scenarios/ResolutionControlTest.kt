package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.library.GatherCardsExecutor
import com.wingedsheep.engine.handlers.effects.player.ControlPlayerDuringResolutionExecutor
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ResolutionControlTest : FunSpec({
    val foe = EffectTarget.PlayerRef(Player.AnOpponent)
    val probe = card("Resolution Control Probe") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                run(Effects.ControlPlayerDuringResolution(foe))
                run(Effects.May(Effects.DrawCards(1, foe), decisionMaker = foe, prompt = "Draw a card?"))
                run(Effects.May(Effects.DrawCards(1, foe), decisionMaker = foe, prompt = "Draw another card?"))
            }
        }
    }
    val abilityProbe = card("Ability Control Probe") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = com.wingedsheep.sdk.dsl.Costs.Free
            effect = Effects.Pipeline {
                run(Effects.ControlPlayerDuringResolution(foe))
                run(Effects.May(Effects.DrawCards(1, foe), decisionMaker = foe))
            }
        }
    }
    val plainProbe = card("Controlled Decision Probe") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell { effect = Effects.May(Effects.DrawCards(1), prompt = "Draw with your own resources?") }
    }
    val synchronous = card("Synchronous Control Probe") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell { effect = Effects.ControlPlayerDuringResolution(foe) }
    }
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(probe, synchronous, plainProbe, abilityProbe))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun cast(d: GameTestDriver, name: String = probe.name) = d.putCardInHand(d.activePlayer!!, name).also {
        d.giveMana(d.activePlayer!!, Color.RED, 1)
        d.castSpell(d.activePlayer!!, it).error shouldBe null
    }
    fun grant(d: GameTestDriver, spell: com.wingedsheep.sdk.model.EntityId, controller: com.wingedsheep.sdk.model.EntityId = d.activePlayer!!): EffectResult =
        ControlPlayerDuringResolutionExecutor().execute(d.state,
            ControlPlayerDuringResolutionEffect(foe, EffectTarget.Self),
            EffectContext(sourceId = spell, controllerId = controller))
    fun visibility(d: GameTestDriver) = Visibility(d.cardRegistry, conditionEvaluator = d.services.predicateEvaluator.conditions)
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }

    test("control is inactive while waiting, survives successive pauses, and ends before priority") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        val id = cast(d)
        d.state.actorFor(victim) shouldBe victim
        d.bothPass()
        d.pendingDecision!!.playerId shouldBe victim
        d.state.actorFor(victim) shouldBe owner
        val originalHand = d.state.getHand(victim).size
        d.submitYesNo(victim, true).error shouldBe null
        d.state.actorFor(victim) shouldBe owner
        d.state.getHand(victim).size shouldBe originalHand + 1
        d.submitYesNo(victim, false).error shouldBe null
        d.state.actorFor(victim) shouldBe victim
        d.state.resolutionControls shouldBe emptyList()
        d.state.continuationStack shouldBe emptyList()
        (id in d.state.getGraveyard(owner)) shouldBe true
    }
    test("synchronous control is removed with start and end events and no extra continuation") {
        val d = driver(); val victim = d.getOpponent(d.activePlayer!!)
        val id = cast(d, synchronous.name)
        val result = d.services.stackResolver.resolveTop(d.state)
        result.error shouldBe null
        result.events.filterIsInstance<ResolutionControlEvent>().map { it.stage } shouldBe
            listOf(ResolutionControlEvent.Stage.GRANTED, ResolutionControlEvent.Stage.ENDED)
        result.state.actorFor(victim) shouldBe victim
        result.state.continuationStack shouldBe emptyList()
        result.state.resolutionControls shouldBe emptyList()
        (id in result.state.stack) shouldBe false
    }
    test("a future spell activates only during resolution and ends after its decisions") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        val id = cast(d)
        val result = grant(d, id)
        d.replaceState(result.state)
        d.state.actorFor(victim) shouldBe victim
        d.bothPass()
        d.state.actorFor(victim) shouldBe owner
        d.submitYesNo(victim, false); d.submitYesNo(victim, false)
        d.state.actorFor(victim) shouldBe victim
    }
    test("countered pending spell drops the grant and a later stack visit does not inherit it") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        val id = cast(d)
        d.replaceState(grant(d, id).state)
        val old = d.state.objectRef(id)!!
        val moved = d.services.spellCounterer.counterSpell(d.state, id).state
            .moveToZone(id, ZoneKey(owner, Zone.GRAVEYARD), ZoneKey(owner, Zone.HAND))
        d.replaceState(moved)
        d.state.isCurrentObject(old) shouldBe false
        d.state.resolutionControls shouldBe emptyList()
        d.state.actorFor(victim) shouldBe victim
    }
    test("a spell with all targets illegal never activates scheduled control or reveals a new hand card") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        val creature = d.putCreatureOnBattlefield(owner, "Grizzly Bears")
        val spell = d.putCardInHand(owner, "Shock")
        d.giveMana(owner, Color.RED, 1)
        d.castSpellWithTargets(owner, spell,
            listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        d.replaceState(grant(d, spell).state)
        val secret = d.putCardInHand(victim, "Shock")
        d.replaceState(d.state.moveToZone(creature, ZoneKey(owner, Zone.BATTLEFIELD), ZoneKey(owner, Zone.GRAVEYARD)))
        val result = d.services.stackResolver.resolveTop(d.state)
        result.error shouldBe null
        result.events.filterIsInstance<ResolutionControlEvent>().any {
            it.stage == ResolutionControlEvent.Stage.STARTED
        } shouldBe false
        result.state.resolutionControls shouldBe emptyList()
        visibility(d).isCardIdentityVisibleTo(result.state, Zone.HAND, secret, owner) shouldBe false
    }
    test("paused state and lifecycle events round trip without losing authority") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val encoded = json.encodeToString(d.state)
        d.replaceState(json.decodeFromString<GameState>(encoded))
        d.state.actorFor(victim) shouldBe owner
        val event: GameEvent = ResolutionControlEvent(d.state.resolutionControls.single(), ResolutionControlEvent.Stage.GRANTED)
        json.decodeFromString<GameEvent>(json.encodeToString(event)) shouldBe event
        d.submitYesNo(victim, false); d.submitYesNo(victim, false)
        d.state.actorFor(victim) shouldBe victim
    }
    test("controlled hand and private library looks are shared, sideboard and spectators stay private") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val hand = d.state.getHand(victim).first()
        val library = d.state.getLibrary(victim).first()
        d.replaceState(d.state.updateEntity(library) { it.with(RevealedToComponent(setOf(victim))) })
        val v = visibility(d)
        v.isCardIdentityVisibleTo(d.state, ZoneKey(victim, Zone.HAND), hand, owner) shouldBe true
        v.isCardIdentityVisibleTo(d.state, ZoneKey(victim, Zone.LIBRARY), library, owner) shouldBe true
        v.isCardIdentityVisibleTo(d.state, ZoneKey(victim, Zone.LIBRARY), library, owner, isSpectator = true) shouldBe false
        v.isZoneVisibleTo(d.state, ZoneKey(victim, Zone.SIDEBOARD), owner) shouldBe false
        v.isZoneVisibleTo(d.state, ZoneKey(victim, Zone.SIDEBOARD), victim) shouldBe true
        d.submitYesNo(victim, false); d.submitYesNo(victim, false)
        v.isCardIdentityVisibleTo(d.state, ZoneKey(victim, Zone.HAND), hand, owner) shouldBe true
        val unseen = d.putCardInHand(victim, "Shock")
        v.isCardIdentityVisibleTo(d.state, ZoneKey(victim, Zone.HAND), unseen, owner) shouldBe false
        v.isCardIdentityVisibleTo(d.state, ZoneKey(victim, Zone.LIBRARY), library, owner) shouldBe false
    }
    test("client banners and affected seat are driven by the shared authority seam") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val transform = ClientStateTransformer(d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        transform.transform(d.state, owner).youAreHijacking shouldBe victim
        transform.transform(d.state, victim).youAreHijackedBy shouldBe owner
        d.pendingDecision!!.playerId shouldBe victim
    }
    test("out-of-game gathering is empty during rules control but hotseat still permits it") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        val side = d.putCardInHand(victim, "Mountain")
        d.replaceState(d.state.moveToZone(side, ZoneKey(victim, Zone.HAND), ZoneKey(victim, Zone.SIDEBOARD)))
        cast(d); d.bothPass()
        val gather = GatherCardsExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry))
        val effect = GatherCardsEffect(CardSource.FromZone(Zone.SIDEBOARD, Player.You), "outside")
        val ctx = EffectContext(sourceId = null, controllerId = victim)
        gather.execute(d.state, effect, ctx).updatedCollections["outside"] shouldBe emptyList()
        val mixed = GatherCardsEffect(CardSource.FromMultipleZones(listOf(Zone.LIBRARY, Zone.SIDEBOARD), Player.You), "mixed")
        gather.execute(d.state, mixed, ctx).updatedCollections["mixed"] shouldBe d.state.getLibrary(victim)
        d.submitYesNo(victim, false); d.submitYesNo(victim, false)
        d.replaceState(d.state.updateEntity(victim) { it.with(HotseatControlComponent(owner)) })
        gather.execute(d.state, effect, ctx).updatedCollections["outside"] shouldBe listOf(side)
        gather.execute(d.state, mixed, ctx).updatedCollections["mixed"] shouldBe d.state.getLibrary(victim) + side
        visibility(d).isZoneVisibleTo(d.state, ZoneKey(victim, Zone.SIDEBOARD), owner) shouldBe true
    }
    test("latest grant wins and ending resolution restores underlying turn control") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val ref = d.state.resolutionControls.single().resolvingObject
        d.replaceState(d.state.copy(resolutionControls = d.state.resolutionControls + ResolutionControl(ref, victim, victim))
            .updateEntity(victim) { it.with(PlayerTurnHijackedComponent(owner, PlayerTurnHijackedComponent.HijackState.ACTIVE)) })
        d.state.actorFor(victim) shouldBe victim
        d.state.endResolutionControl(ref).state.actorFor(victim) shouldBe owner
    }
    test("nested lifetime completion restores an outer window without deleting its grant") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val outer = d.state.resolutionControls.single().resolvingObject
        val inner = ObjectRef(com.wingedsheep.sdk.model.EntityId.generate(), 999)
        val nested = d.state.copy(
            resolutionControls = d.state.resolutionControls + ResolutionControl(inner, victim, victim),
            continuationStack = d.state.continuationStack.dropLast(1) + EndResolutionControlContinuation(inner) + d.state.continuationStack.takeLast(1))
        nested.actorFor(victim) shouldBe victim
        val restored = nested.endResolutionControl(inner).state
        restored.actorFor(victim) shouldBe owner
        restored.resolutionControls.single().resolvingObject shouldBe outer
    }
    test("non-stack object and nonexistent player do not gain control or emit a grant") {
        val d = driver(); val victim = d.getOpponent(d.activePlayer!!)
        val land = d.putCardInHand(d.activePlayer!!, "Mountain")
        val result = grant(d, land)
        result.events shouldBe emptyList()
        result.state.actorFor(victim) shouldBe victim
    }
    test("a scheduled opponent-owned spell retains its caster and draws for that player") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        val id = d.putCardInHand(victim, plainProbe.name)
        d.replaceState(d.state.withPriority(victim)); d.giveMana(victim, Color.RED, 1)
        d.castSpell(victim, id).error shouldBe null
        d.replaceState(grant(d, id).state)
        d.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>()?.casterId shouldBe victim
        d.bothPass()
        d.state.actorFor(victim) shouldBe owner
        d.pendingDecision!!.playerId shouldBe victim
        val hand = d.state.getHand(victim).size
        val drawn = d.submitYesNo(victim, true)
        drawn.error shouldBe null
        val drawEvent = drawn.events.filterIsInstance<CardsDrawnEvent>().single()
        (owner in drawEvent.identityViewers) shouldBe true
        val clientDraw = com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(drawEvent), owner, d.state)
            .single() as com.wingedsheep.engine.view.ClientEvent.CardDrawn
        clientDraw.cardName shouldBe "Mountain"
        clientDraw.isYours shouldBe false
        clientDraw.description shouldBe "Opponent drew Mountain"
        val hiddenDraw = com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(drawEvent), com.wingedsheep.sdk.model.EntityId.generate(), d.state)
            .single() as com.wingedsheep.engine.view.ClientEvent.CardDrawn
        hiddenDraw.cardName shouldBe null
        json.decodeFromString<GameEvent>(json.encodeToString<GameEvent>(drawEvent)) shouldBe drawEvent
        d.state.getHand(victim).size shouldBe hand + 1
        d.state.actorFor(victim) shouldBe victim
        d.state.getEntity(victim)?.get<ManaPoolComponent>()?.total shouldBe 0
    }
    test("permanent copy choices are controlled only during entry, not its later battlefield life") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        d.putCreatureOnBattlefield(owner, "Grizzly Bears")
        val id = d.putCardInHand(victim, "Clone")
        d.replaceState(d.state.copy(activePlayerId = victim).withPriority(victim)); d.giveMana(victim, Color.BLUE, 4)
        d.castSpell(victim, id).error shouldBe null
        d.replaceState(grant(d, id, owner).state)
        d.bothPass()
        d.state.actorFor(victim) shouldBe owner
        d.pendingDecision!!.playerId shouldBe victim
        d.submitCardSelection(victim, emptyList()).error shouldBe null
        d.state.actorFor(victim) shouldBe victim
        d.state.resolutionControls shouldBe emptyList()
        d.state.continuationStack shouldBe emptyList()
    }
    test("shared-turn teams receive the same resolution control window") {
        val d = GameTestDriver(); d.registerCards(TestCards.all + listOf(plainProbe))
        val players = d.initMultiplayer(List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(), teams = listOf(listOf(0, 1), listOf(2, 3)))
        val owner = players[0]; val victim = players[2]
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val id = d.putCardInHand(owner, plainProbe.name); d.giveMana(owner, Color.RED, 1)
        d.castSpell(owner, id).error shouldBe null
        val result = ControlPlayerDuringResolutionExecutor().execute(d.state,
            ControlPlayerDuringResolutionEffect(EffectTarget.ContextTarget(0), EffectTarget.Self),
            EffectContext(sourceId = id, controllerId = owner, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(victim))))
        val active = result.state.pushContinuation(EndResolutionControlContinuation(d.state.objectRef(id)!!))
        active.actorFor(victim) shouldBe owner
        active.actorFor(players[3]) shouldBe owner
        active.endResolutionControl(d.state.objectRef(id)!!).state.actorFor(players[3]) shouldBe players[3]
    }
    test("controller departure ends authority without abandoning another player's question") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val result = com.wingedsheep.engine.mechanics.sba.player.PlayerLeavesGameProcessor.process(
            d.services.zones, d.state, owner, GameEndReason.CONCESSION)
        result.state.actorFor(victim) shouldBe victim
        result.state.resolutionControls shouldBe emptyList()
        result.state.pendingDecision!!.playerId shouldBe victim
        result.events.filterIsInstance<ResolutionControlEvent>().last().stage shouldBe ResolutionControlEvent.Stage.ENDED
    }
    test("internal control lifecycle events are not forwarded to clients") {
        val d = driver(); cast(d); d.bothPass()
        val event: GameEvent = ResolutionControlEvent(d.state.resolutionControls.single(), ResolutionControlEvent.Stage.GRANTED)
        com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(event), d.activePlayer!!, d.state) shouldBe emptyList()
    }
    test("an ability controls during its own resolution even after its source leaves") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        val source = d.putPermanentOnBattlefield(owner, abilityProbe.name)
        d.submitSuccess(ActivateAbility(owner, source, abilityProbe.script.activatedAbilities.single().id))
        val ability = d.state.stack.last()
        d.replaceState(d.state.moveToZone(source, ZoneKey(owner, Zone.BATTLEFIELD), ZoneKey(owner, Zone.GRAVEYARD)))
        d.bothPass()
        d.state.actorFor(victim) shouldBe owner
        d.state.resolutionControls.single().resolvingObject.entityId shouldBe ability
        d.submitYesNo(victim, false).error shouldBe null
        d.state.actorFor(victim) shouldBe victim
        d.state.resolutionControls shouldBe emptyList()
    }
    test("an empty control state stays absent in replay serialization") {
        val d = driver()
        json.parseToJsonElement(json.encodeToString(d.state)).toString().contains("resolutionControls") shouldBe false
    }
    test("private library look memory includes the authorized controller after the window ends") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val gather = GatherCardsExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry))
        val result = gather.execute(d.state, GatherCardsEffect(CardSource.TopOfLibrary(com.wingedsheep.sdk.dsl.DynamicAmounts.fixed(1), Player.You), "looked"),
            EffectContext(sourceId = null, controllerId = victim))
        d.replaceState(result.state)
        val looked = result.updatedCollections["looked"]!!.single()
        d.submitYesNo(victim, false); d.submitYesNo(victim, false)
        visibility(d).isCardIdentityVisibleTo(d.state, ZoneKey(victim, Zone.LIBRARY), looked, owner) shouldBe true
        visibility(d).isCardIdentityVisibleTo(d.state, ZoneKey(victim, Zone.LIBRARY), looked, com.wingedsheep.sdk.model.EntityId.generate()) shouldBe false
    }
    test("private hand look captures its observer without revealing to other connections") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val result = com.wingedsheep.engine.handlers.effects.information.LookAtTargetHandExecutor().execute(d.state,
            LookAtTargetHandEffect(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = null, controllerId = victim, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(owner))))
        val event = result.events.filterIsInstance<HandLookedAtEvent>().single()
        (owner in event.identityViewers) shouldBe true
        d.replaceState(result.state); d.submitYesNo(victim, false); d.submitYesNo(victim, false)
        com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(event), owner, d.state).size shouldBe 1
        com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(event), com.wingedsheep.sdk.model.EntityId.generate(), d.state) shouldBe emptyList()
    }
    test("known library placements retain controller knowledge while shuffled placements remain hidden") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        val known = d.putCardInHand(victim, "Shock")
        val hidden = d.putCardInHand(victim, "Grizzly Bears")
        val shuffled = d.services.zones.moveToZone(d.state, hidden, Zone.LIBRARY,
            com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(
                libraryPlacement = com.wingedsheep.engine.handlers.effects.LibraryPlacement.Shuffled,
                libraryMoverId = victim))
        val placed = d.services.zones.moveToZone(shuffled.state, known, Zone.LIBRARY,
            com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(
                libraryPlacement = com.wingedsheep.engine.handlers.effects.LibraryPlacement.Top,
                libraryMoverId = victim))
        d.replaceState(placed.state)
        d.submitYesNo(victim, false); d.submitYesNo(victim, false)
        val v = visibility(d)
        v.isCardIdentityVisibleTo(d.state, Zone.LIBRARY, known, owner) shouldBe true
        v.isCardIdentityVisibleTo(d.state, Zone.LIBRARY, known, victim) shouldBe true
        v.isCardIdentityVisibleTo(d.state, Zone.LIBRARY, known, com.wingedsheep.sdk.model.EntityId.generate()) shouldBe false
        v.isCardIdentityVisibleTo(d.state, Zone.LIBRARY, hidden, owner) shouldBe false
        val shuffledAgain = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils.clearLibraryReveals(d.state, victim)
        v.isCardIdentityVisibleTo(shuffledAgain, Zone.LIBRARY, known, owner) shouldBe false
    }
    test("session hotseat precedence remains even during a resolution grant") {
        val d = driver(); val owner = d.activePlayer!!; val victim = d.getOpponent(owner)
        cast(d); d.bothPass()
        d.replaceState(d.state.updateEntity(victim) { it.with(HotseatControlComponent(victim)) })
        d.state.actorFor(victim) shouldBe victim
    }
})
