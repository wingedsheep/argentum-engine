package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

class SourceZoneIdentityTest : FunSpec({
    val returning = card("Identity Returning Faerie") {
        manaCost = "{U}"
        typeLine = "Creature — Faerie"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.Dies
            effect = Patterns.Mechanic.clash(Effects.ReturnToHandFromGraveyard(EffectTarget.Self))
        }
    }
    val boulder = card("Identity Boulder") { manaCost = "{5}"; typeLine = "Artifact"; oracleText = "" }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(returning, boulder))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun roundTrip(d: GameTestDriver) {
        val json = Json { serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), d.state)))
    }
    fun move(d: GameTestDriver, id: EntityId, from: Zone, to: Zone) {
        d.replaceState(d.state.removeFromZone(ZoneKey(d.player1, from), id).addToZone(ZoneKey(d.player1, to), id))
    }
    for (change in listOf("none", "exile", "roundtrip", "token")) {
        test("dies source $change still clashes but only the captured graveyard object returns") {
            val d = driver()
            val source = d.putCreatureOnBattlefield(d.player1, returning.name)
            if (change == "token") d.replaceState(d.state.updateEntity(source) { it.with(TokenComponent) })
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, bolt, listOf(source)).error shouldBe null
            d.bothPass().error shouldBe null
            d.stackSize shouldBe 1
            if (change == "exile" || change == "roundtrip") move(d, source, Zone.GRAVEYARD, Zone.EXILE)
            if (change == "roundtrip") move(d, source, Zone.EXILE, Zone.GRAVEYARD)
            d.putCardOnTopOfLibrary(d.player1, boulder.name)
            d.putCardOnTopOfLibrary(d.player2, "Plains")
            roundTrip(d)
            d.bothPass().error shouldBe null
            repeat(2) {
                val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                roundTrip(d)
                d.submitCardSelection(choice.playerId, emptyList()).error shouldBe null
            }
            d.pendingDecision shouldBe null
            (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe (change == "none")
            if (change == "token") d.state.getEntity(source) shouldBe null
            if (change == "roundtrip") {
                (source in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe true
                d.state.getEntity(source)?.get<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>() shouldBe null
            }
        }
    }

    for (intervene in listOf(false, true)) {
        test("same-resolution public move survives serialized choice; unrelated move=$intervene invalidates it") {
            val d = driver()
            val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val ref = d.state.objectRef(source)!!
            val context = EffectContext(sourceId = source, controllerId = d.player1,
                objectReferences = ObjectReferenceEnvironment(captured = true, origin = ref, source = ref, resolutionKey = "identity-inline-resolution"))
            val effect = Effects.Composite(
                Effects.Exile(EffectTarget.Self),
                GatedEffect(Gate.MayDecide("Continue?"), Effects.ReturnToHand(EffectTarget.Self)),
            )
            val result = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state, effect, context)
            result.error shouldBe null
            d.replaceState(result.state)
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            if (intervene) { move(d, source, Zone.EXILE, Zone.GRAVEYARD); move(d, source, Zone.GRAVEYARD, Zone.EXILE) }
            roundTrip(d)
            d.submitYesNo(d.player1, true).error shouldBe null
            (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe !intervene
            (source in d.state.getZone(ZoneKey(d.player1, Zone.EXILE))) shouldBe intervene
        }
    }

    test("legacy ability lacking historical refs fails closed without erasing source identity metadata") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val context = EffectContext(sourceId = source, controllerId = d.player1).forAbilityResolution(d.state)
        context.sourceId shouldBe source
        context.resolveTarget(EffectTarget.Self, d.state) shouldBe null
    }

    for (intervene in listOf(false, true)) {
        test("self-sacrifice cost preserves original source and authorizes only its immediate destination; intervene=$intervene") {
            val d = driver()
            val sacrifice = card("Identity Sacrifice Return") {
                manaCost = "{U}"; typeLine = "Creature — Spirit"; power = 1; toughness = 1
                activatedAbility {
                    cost = Costs.SacrificeSelf
                    effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
                }
            }
            d.registerCards(listOf(sacrifice))
            val source = d.putCreatureOnBattlefield(d.player1, sacrifice.name)
            val origin = d.state.objectRef(source)
            d.submit(ActivateAbility(d.player1, source, sacrifice.activatedAbilities.single().id)).error shouldBe null
            val stackAbility = d.state.getEntity(d.state.stack.last())!!
                .get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>()!!
            stackAbility.objectReferences.origin shouldBe origin
            if (intervene) { move(d, source, Zone.GRAVEYARD, Zone.EXILE); move(d, source, Zone.EXILE, Zone.GRAVEYARD) }
            roundTrip(d)
            d.bothPass().error shouldBe null
            (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe !intervene
        }
    }

    test("invalid triggering object preserves its last-known power and controller") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val ref = d.state.objectRef(source)!!
        val context = EffectContext(sourceId = null, controllerId = d.player2,
            triggeringEntityId = source, triggeringPlayerId = d.player1, triggerLastKnownPower = 7,
            objectReferences = ObjectReferenceEnvironment(captured = true, triggering = ref))
        move(d, source, Zone.BATTLEFIELD, Zone.EXILE)
        val current = context.withCurrentObjectReferences(d.state)
        current.resolveTarget(EffectTarget.TriggeringEntity, d.state) shouldBe null
        current.resolveTarget(EffectTarget.ControllerOfTriggeringEntity, d.state) shouldBe d.player1
        com.wingedsheep.engine.handlers.DynamicAmountEvaluator().evaluate(d.state, DynamicAmounts.triggeringPower(), current) shouldBe 7
        for (cardSource in listOf(com.wingedsheep.sdk.scripting.effects.CardSource.Self,
            com.wingedsheep.sdk.scripting.effects.CardSource.TriggeringEntity)) {
            val gathered = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state,
                com.wingedsheep.sdk.scripting.effects.GatherCardsEffect(cardSource, "stale"),
                current.copy(sourceId = source, objectReferences = current.objectReferences.copy(source = ref)))
            gathered.updatedCollections?.get("stale") shouldBe emptyList()
        }
    }

    test("replacement additional movements do not authorize a reference to follow") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val old = d.state.objectRef(source)!!
        move(d, source, Zone.BATTLEFIELD, Zone.EXILE)
        val new = d.state.objectRef(source)!!
        val env = ObjectReferenceEnvironment(captured = true, origin = old, source = old)
            .authorize(listOf(com.wingedsheep.engine.core.ZoneChangeEvent(source, "Grizzly Bears", Zone.BATTLEFIELD,
                Zone.EXILE, d.player1, oldObject = old, newObject = new,
                transitionCause = com.wingedsheep.engine.core.ZoneTransitionCause.REPLACEMENT_ADDITIONAL)))
        env.isCurrent(env.source, d.state) shouldBe false
    }

    test("triggering players retain stable identity without a zone object reference") {
        val d = driver()
        val context = EffectContext(sourceId = null, controllerId = d.player1,
            triggeringEntityId = d.player2,
            objectReferences = ObjectReferenceEnvironment(captured = true))
            .withCurrentObjectReferences(d.state)
        context.triggeringReferenceLost shouldBe false
        context.resolveTarget(EffectTarget.TriggeringEntity, d.state) shouldBe d.player2
    }

    test("ETB self trigger originates on the entered object rather than the old stack object") {
        val d = driver()
        val entrant = card("Identity Entering Spirit") {
            manaCost = "{U}"; typeLine = "Creature — Spirit"; power = 1; toughness = 1
            triggeredAbility { trigger = Triggers.EntersBattlefield; effect = Effects.ReturnToHand(EffectTarget.Self) }
        }
        d.registerCards(listOf(entrant))
        val source = d.putCardInHand(d.player1, entrant.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, source).error shouldBe null
        val stackObject = d.state.objectRef(source)
        d.bothPass().error shouldBe null
        val enteredObject = d.state.objectRef(source)
        (enteredObject == stackObject) shouldBe false
        val trigger = d.state.getEntity(d.state.stack.last())!!
            .get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()!!
        trigger.objectReferences.origin shouldBe enteredObject
        trigger.objectReferences.source shouldBe enteredObject
        roundTrip(d)
        d.bothPass().error shouldBe null
        (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe true
    }

    test("simultaneously dying observer retains battlefield origin when observing another death") {
        val d = driver()
        val observer = card("Identity Death Observer") {
            manaCost = "{B}"; typeLine = "Creature — Spirit"; power = 1; toughness = 1
            triggeredAbility { trigger = Triggers.AnyCreatureDies; effect = Effects.GainLife(1) }
        }
        d.registerCards(listOf(observer))
        val source = d.putCreatureOnBattlefield(d.player1, observer.name)
        val other = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val origin = d.state.objectRef(source)
        val result = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state,
            Effects.DestroyAll(com.wingedsheep.sdk.dsl.Filters.Creature),
            EffectContext(sourceId = null, controllerId = d.player1))
        result.error shouldBe null
        val triggers = com.wingedsheep.engine.event.TriggerDetector(d.cardRegistry)
            .detectTriggers(result.state, result.events)
        val observed = triggers.single { it.sourceId == source && it.triggerContext.triggeringEntityId == other }
        observed.objectReferences.origin shouldBe origin
        (observed.objectReferences.origin == result.state.objectRef(source)) shouldBe false
    }

    for (path in listOf("modal", "pay", "any-pay", "life-bid", "legacy-pause")) {
        test("$path serialized resume preserves source identity and rejects a later incarnation") {
            val d = driver()
            val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val ref = d.state.objectRef(source)!!
            val context = EffectContext(sourceId = source, controllerId = d.player1,
                objectReferences = ObjectReferenceEnvironment(captured = true, origin = ref, source = ref,
                    resolutionKey = "identity-resume-$path"))
            val bounce = Effects.ReturnToHand(EffectTarget.Self)
            val effect = when (path) {
                "modal" -> com.wingedsheep.sdk.scripting.effects.ModalEffect(listOf(
                    com.wingedsheep.sdk.scripting.effects.Mode(bounce),
                    com.wingedsheep.sdk.scripting.effects.Mode(Effects.GainLife(1))))
                "pay" -> com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect(
                    cost = Costs.pay.PayLife(1), suffer = bounce)
                "any-pay" -> com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect(cost = Costs.pay.PayLife(1), consequenceIfNonePaid = bounce)
                "life-bid" -> com.wingedsheep.sdk.scripting.effects.OpenLifeBidEffect(onWin = bounce)
                else -> GatedEffect(Gate.MayDecide("Return?"), bounce)
            }
            val result = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state, effect, context)
            result.error shouldBe null
            d.replaceState(result.state)
            move(d, source, Zone.BATTLEFIELD, Zone.EXILE)
            move(d, source, Zone.EXILE, Zone.BATTLEFIELD)
            if (path == "legacy-pause") {
                val json = Json { serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true }
                fun strip(value: kotlinx.serialization.json.JsonElement): kotlinx.serialization.json.JsonElement = when (value) {
                    is kotlinx.serialization.json.JsonObject -> kotlinx.serialization.json.JsonObject(value.filterKeys { it != "objectReferences" }.mapValues { strip(it.value) })
                    is kotlinx.serialization.json.JsonArray -> kotlinx.serialization.json.JsonArray(value.map(::strip))
                    else -> value
                }
                val encoded = json.encodeToJsonElement(GameState.serializer(), d.state)
                d.replaceState(json.decodeFromJsonElement(GameState.serializer(), strip(encoded)))
            } else roundTrip(d)
            if (path == "modal") {
                val decision = d.pendingDecision.shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseOptionDecision>()
                d.submitDecision(d.player1, com.wingedsheep.engine.core.OptionChosenResponse(decision.id, 0)).error shouldBe null
            } else {
                val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                d.submitYesNo(decision.playerId, path == "legacy-pause").error shouldBe null
                if (path == "any-pay" && d.pendingDecision != null) {
                    val next = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                    d.submitYesNo(next.playerId, false).error shouldBe null
                }
            }
            (source in d.state.getBattlefield()) shouldBe true
            (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe false
        }
    }

    test("source number choice cannot mutate a later source incarnation") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val ref = d.state.objectRef(source)!!
        val result = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state,
            Effects.ChooseNumberForSource(minValue = 0, maxValue = 7),
            EffectContext(sourceId = source, controllerId = d.player1,
                objectReferences = ObjectReferenceEnvironment(captured = true, origin = ref, source = ref)))
        d.replaceState(result.state)
        move(d, source, Zone.BATTLEFIELD, Zone.EXILE)
        move(d, source, Zone.EXILE, Zone.BATTLEFIELD)
        roundTrip(d)
        val decision = d.pendingDecision.shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseNumberDecision>()
        d.submitDecision(d.player1, com.wingedsheep.engine.core.NumberChosenResponse(decision.id, 4)).error shouldBe null
        d.state.getEntity(source)?.get<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>() shouldBe null
    }

    test("any-player sacrifice cost authorizes its own immediate public destination before consequence") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val ref = d.state.objectRef(source)!!
        val result = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state,
            Effects.Composite(
                com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect(
                    cost = Costs.pay.Sacrifice(com.wingedsheep.sdk.dsl.Filters.Creature),
                    consequence = GatedEffect(Gate.MayDecide("Draw?"), Effects.DrawCards(1))),
                Effects.ReturnToHandFromGraveyard(EffectTarget.Self)),
            EffectContext(sourceId = source, controllerId = d.player1,
                objectReferences = ObjectReferenceEnvironment(captured = true, origin = ref, source = ref,
                    resolutionKey = "identity-sacrifice-consequence")))
        result.error shouldBe null
        d.replaceState(result.state)
        roundTrip(d)
        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(decision.playerId, listOf(source)).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        roundTrip(d)
        d.submitYesNo(d.player1, true).error shouldBe null
        (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe true
    }

    test("dredge replacement cannot return a new same-ID graveyard object after serialized choice") {
        val d = driver()
        val dredger = card("Identity Dredger") {
            manaCost = "{G}"; typeLine = "Creature — Plant"; power = 1; toughness = 1
            keywordAbility(com.wingedsheep.sdk.scripting.KeywordAbility.dredge(1))
        }
        d.registerCards(listOf(dredger))
        val source = d.putCreatureOnBattlefield(d.player1, dredger.name)
        move(d, source, Zone.BATTLEFIELD, Zone.GRAVEYARD)
        val result = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state,
            Effects.DrawCards(1), EffectContext(sourceId = null, controllerId = d.player1))
        result.error shouldBe null
        d.replaceState(result.state)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        move(d, source, Zone.GRAVEYARD, Zone.EXILE)
        move(d, source, Zone.EXILE, Zone.GRAVEYARD)
        roundTrip(d)
        d.submitYesNo(d.player1, true).error shouldBe null
        (source in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe true
        (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe false
    }

    for (intervene in listOf(false, true)) {
        test("delayed Self retains creation-time source identity across serialization; intervene=$intervene") {
            val d = driver()
            val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val original = d.state.objectRef(source)!!
            val result = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state,
                com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect(
                    step = Step.END, effect = Effects.ReturnToHand(EffectTarget.Self)),
                EffectContext(sourceId = source, controllerId = d.player1,
                    objectReferences = ObjectReferenceEnvironment(captured = true, origin = original, source = original)))
            result.error shouldBe null
            d.replaceState(result.state)
            if (intervene) { move(d, source, Zone.BATTLEFIELD, Zone.EXILE); move(d, source, Zone.EXILE, Zone.BATTLEFIELD) }
            roundTrip(d)
            d.state.delayedTriggers.single().objectReferences.origin shouldBe original
            d.passPriorityUntil(Step.END)
            d.stackSize shouldBe 1
            roundTrip(d)
            d.bothPass().error shouldBe null
            (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe !intervene
        }
    }

    for (case in listOf("sba", "simultaneous", "exile", "roundtrip")) {
        test("attached death trigger separates Aura origin and immediate graveyard objects: $case") {
            val d = driver()
            val aura = card("Identity Returning Aura") {
                manaCost = "{U}"; typeLine = "Enchantment — Aura"
                triggeredAbility {
                    trigger = Triggers.leavesBattlefield(to = Zone.GRAVEYARD,
                        binding = com.wingedsheep.sdk.scripting.TriggerBinding.ATTACHED)
                    effect = Effects.Composite(Effects.ReturnToHandFromGraveyard(EffectTarget.Self),
                        Effects.ReturnToHandFromGraveyard(EffectTarget.TriggeringEntity))
                }
            }
            val sweep = card("Identity Sweep") {
                manaCost = "{R}"; typeLine = "Sorcery"
                spell { effect = Effects.DestroyAll(com.wingedsheep.sdk.dsl.Filters.Permanent) }
            }
            d.registerCards(listOf(aura, sweep))
            val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val source = d.putPermanentOnBattlefield(d.player1, aura.name)
            d.replaceState(d.state.updateEntity(source) { it.with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(host)) }
                .updateEntity(host) { it.with(com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(listOf(source))) })
            val origin = d.state.objectRef(source)
            val spell = d.putCardInHand(d.player1, if (case == "simultaneous") sweep.name else "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, spell, if (case == "simultaneous") emptyList() else listOf(host)).error shouldBe null
            d.bothPass().error shouldBe null
            d.stackSize shouldBe 1
            val trigger = d.state.getEntity(d.state.stack.last())!!
                .get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()!!
            trigger.objectReferences.origin shouldBe origin
            trigger.objectReferences.source shouldBe d.state.objectRef(source)
            trigger.objectReferences.triggering shouldBe d.state.objectRef(host)
            if (case == "exile" || case == "roundtrip") move(d, source, Zone.GRAVEYARD, Zone.EXILE)
            if (case == "roundtrip") move(d, source, Zone.EXILE, Zone.GRAVEYARD)
            roundTrip(d)
            d.bothPass().error shouldBe null
            (host in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe true
            (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe (case == "sba" || case == "simultaneous")
        }
    }

    test("reflected spell damage retains event-time caster after the opponent-owned spell leaves the stack") {
        val d = driver()
        val reflection = card("Identity Reflected Spell") {
            manaCost = "{W}"; typeLine = "Instant"
            spell { effect = Effects.ReflectNextDamageFromChosenSourceToController() }
        }
        d.registerCards(listOf(reflection))
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        // The caster holds a card owned by the other player. Its graveyard owner is not its controller on the stack.
        d.replaceState(d.state.removeFromZone(ZoneKey(d.player2, Zone.HAND), bolt)
            .addToZone(ZoneKey(d.player1, Zone.HAND), bolt))
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(d.player2)).error shouldBe null
        val stackSpell = d.state.objectRef(bolt)
        d.passPriority(d.player1).error shouldBe null
        val response = d.putCardInHand(d.player2, reflection.name)
        d.giveMana(d.player2, Color.WHITE, 1)
        d.castSpell(d.player2, response).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player2, listOf(bolt)).error shouldBe null
        d.bothPass().error shouldBe null
        d.stackSize shouldBe 1
        val trigger = d.state.getEntity(d.state.stack.last())!!
            .get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()!!
        trigger.objectReferences.triggering shouldBe stackSpell
        trigger.triggeringPlayerId shouldBe d.player1
        EffectContext(sourceId = trigger.sourceId, controllerId = d.player2, triggeringEntityId = bolt,
            objectReferences = trigger.objectReferences).resolveTarget(EffectTarget.TriggeringEntity, d.state) shouldBe null
        (bolt in d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD))) shouldBe true
        roundTrip(d)
        d.bothPass().error shouldBe null
        d.assertLifeTotal(d.player1, 17)
        d.assertLifeTotal(d.player2, 17)
    }

    for (pauseAction in listOf(false, true)) for (intervene in listOf(false, true)) {
        test("reflexive action preserves exact source through stack and serialization; pause=$pauseAction intervene=$intervene") {
            val d = driver()
            val subject = card("Identity Reflexive Spirit") {
                manaCost = "{U}"; typeLine = "Creature — Spirit"; power = 1; toughness = 1
                triggeredAbility {
                    trigger = Triggers.EntersBattlefield
                    effect = com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect(
                        action = if (pauseAction) Effects.Composite(
                            GatedEffect(Gate.MayDecide("Draw?"), Effects.DrawCards(1)), Effects.SacrificeTarget(EffectTarget.Self)
                        ) else Effects.SacrificeTarget(EffectTarget.Self),
                        optional = false,
                        reflexiveEffect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
                    )
                }
            }
            d.registerCards(listOf(subject))
            val source = d.putCardInHand(d.player1, subject.name)
            d.giveMana(d.player1, Color.BLUE, 1)
            d.castSpell(d.player1, source).error shouldBe null
            d.bothPass().error shouldBe null
            val origin = d.state.objectRef(source)
            d.bothPass().error shouldBe null
            if (pauseAction) {
                roundTrip(d)
                d.submitYesNo(d.player1, true).error shouldBe null
            }
            (source in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe true
            d.stackSize shouldBe 1
            val trigger = d.state.getEntity(d.state.stack.last())!!
                .get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()!!
            trigger.objectReferences.origin shouldBe origin
            trigger.objectReferences.followed(trigger.objectReferences.source!!) shouldBe d.state.objectRef(source)
            if (intervene) { move(d, source, Zone.GRAVEYARD, Zone.EXILE); move(d, source, Zone.EXILE, Zone.GRAVEYARD) }
            roundTrip(d)
            d.bothPass().error shouldBe null
            (source in d.state.getZone(ZoneKey(d.player1, Zone.HAND))) shouldBe !intervene
        }
    }

})
