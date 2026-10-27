package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.EffectProgram
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RestrictCardsDrawnPerTurn
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EachPlayerChoosesCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.SecretChoice
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Pausing never changes what an effect does. Each shape runs twice, with and without the
 * selection pause: once where its selection resolves on its own (select every eligible card, of
 * which there is one), once where the same pick asks and gets the same card. Both runs must end in the same game,
 * and the run without the selection pause is the reference. (Some shapes also ask other questions —
 * a "may", a secret number — in both runs.)
 */
class EffectProgramPauseTwinTest : FunSpec({

    /**
     * Pick the one creature card in hand; asks only when [prompt]. Stores "pool" and "picked". A
     * choice over a filtered hand is always asked (#3340), so the silent twin selects all instead.
     */
    fun pick(prompt: Boolean): List<Effect> = listOf(
        GatherCardsEffect(source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature), storeAs = "pool"),
        SelectFromCollectionEffect(
            from = "pool",
            selection = if (prompt) SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)) else SelectionMode.All,
            storeSelected = "picked", alwaysPrompt = prompt,
        ),
    )

    fun store(name: String, value: Int) = Effects.StoreNumber(name, DynamicAmount.Fixed(value))
    val gainN = Effects.GainLife(DynamicAmount.VariableReference("n"))
    // A gain with no player to gain it fails: the ability has no targets to read.
    val failing = Effects.GainLife(10, EffectTarget.ContextTarget(0))

    /** "If [collection] has a card in hand matching [predicate], gain 4." */
    fun ifHandHas(predicate: CardPredicate) = GatedEffect(
        gate = Gate.DoAction(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter(cardPredicates = listOf(predicate))),
                storeAs = "matched",
            ),
            SuccessCriterion.CollectionNonEmpty("matched"),
        ),
        then = Effects.GainLife(4),
    )

    // 1. An inner composite shadows an outer key; only new keys leave it.
    fun shadowLast(p: Boolean) = CompositeEffect(listOf(
        store("n", 1), CompositeEffect(listOf(store("n", 2)) + pick(p)), gainN,
    ))
    fun shadowFollowed(p: Boolean) = CompositeEffect(listOf(
        store("n", 1), CompositeEffect(listOf(store("n", 2)) + pick(p) + store("other", 0)), gainN,
    ))
    // 2. A cost-then-payoff composite whose cost fails after a selection.
    fun costAfterSelection(p: Boolean) = CompositeEffect(listOf(
        CompositeEffect(pick(p) + failing + Effects.GainLife(5), stopOnError = true),
        Effects.GainLife(1),
    ))
    // 3. A DoAction gate reads a collection its action stored before the action's final leaf.
    fun doActionReadsEarlier(p: Boolean) = GatedEffect(
        gate = Gate.DoAction(CompositeEffect(pick(p)), SuccessCriterion.CollectionNonEmpty("pool")),
        then = Effects.GainLife(3),
    )
    // 4. Subtype groups gathered before a selection, read after the composite they were stored in.
    fun subtypeGroups(p: Boolean) = CompositeEffect(listOf(
        CompositeEffect(listOf(
            pick(p)[0],
            GatherSubtypesEffect(from = "pool", storeAs = "groups"),
            pick(p)[1],
        )),
        ifHandHas(CardPredicate.HasSubtypeInEachStoredGroup("groups")),
    ))
    // 5. A not-yet-migrated control leaf inside a composite: a "may" whose branch stores a number
    // (and, below, a subtype group) that a later sibling reads.
    fun mayBranchStores(p: Boolean) = CompositeEffect(listOf(
        Effects.May(CompositeEffect(pick(p) + store("n", 3))), gainN,
    ))
    fun mayBranchSubtypes(p: Boolean) = CompositeEffect(listOf(
        Effects.May(CompositeEffect(listOf(pick(p)[0], GatherSubtypesEffect(from = "pool", storeAs = "groups"), pick(p)[1]))),
        ifHandHas(CardPredicate.HasSubtypeInEachStoredGroup("groups")),
    ))
    // 5b. A resumer that runs the tree it built after an answer: its branch stores a number that a
    // later sibling reads, whether the branch finishes in the resumer or pauses inside it.
    fun chooseNumberThenStores(p: Boolean) = CompositeEffect(listOf(
        Effects.ChooseNumberThen(CompositeEffect(pick(p) + store("n", 3)), maxValue = 3), gainN,
    ))
    // 6. A program queued by an executor, not started when it is pushed; it starts after the
    // secret number and pauses on the selection.
    fun queued(p: Boolean): Effect = Effects.SecretChoice(
        SecretChoice.Number(minValue = 0, maxValue = DynamicAmount.Fixed(3)),
        participants = listOf(Player.You),
        then = shadowFollowed(p),
    )

    fun artifact(name: String, effect: Effect): CardDefinition = card(name) {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            this.effect = effect
        }
    }

    data class Shape(val name: String, val build: (Boolean) -> Effect, val expectedLife: Int)
    val shapes = listOf(
        // The inner n = 2 doesn't leave its composite: gain 1.
        Shape("shadowed key, pause last in the inner composite", ::shadowLast, expectedLife = 21),
        Shape("shadowed key, pause followed by a step", ::shadowFollowed, expectedLife = 21),
        // The failed cost abandons the payoff (5); the step after the composite (1) runs.
        Shape("stopOnError around a selection, then a failing cost", ::costAfterSelection, expectedLife = 21),
        // "pool" was stored, so the gate's action happened: gain 3.
        Shape("DoAction consumer of an earlier collection", ::doActionReadsEarlier, expectedLife = 23),
        // The bear's subtypes were gathered, and the bear matches them: gain 4.
        Shape("subtype groups across a paused leaf", ::subtypeGroups, expectedLife = 24),
        // The may's branch stores n = 3 and a subtype group; the siblings after the may read them.
        Shape("may branch stores a number", ::mayBranchStores, expectedLife = 23),
        Shape("may branch stores a subtype group", ::mayBranchSubtypes, expectedLife = 24),
        Shape("choose-number-then branch stores a number", ::chooseNumberThenStores, expectedLife = 23),
        Shape("not-started program queued by an executor", ::queued, expectedLife = 21),
    )
    val cards = shapes.flatMap { shape ->
        listOf(false, true).map { p -> artifact("Twin ${shape.name} ${if (p) "asks" else "silent"}", shape.build(p)) }
    }
    // Resolution depth probes: the depth a "may" reports, at the root composite, one composite
    // deeper, and at the root after a pause.
    val mayGain = Effects.May(Effects.GainLife(1))
    val depthRoot = artifact("Twin depth root", CompositeEffect(listOf(mayGain, store("other", 0))))
    val depthNested = artifact("Twin depth nested", CompositeEffect(listOf(CompositeEffect(listOf(mayGain, store("other", 0))), store("other", 0))))
    val depthAfterPause = artifact("Twin depth after pause", CompositeEffect(pick(true) + mayGain + store("other", 0)))
    // A live cursor whose deltas carry every kind: a number, a sacrifice snapshot, a departed
    // snapshot and a permit (the source exiled) are all stored before the nested composite opens.
    fun liveCursor(p: Boolean) = CompositeEffect(listOf(
        store("n", 1),
        Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.Controller),
        Effects.Exile(EffectTarget.Self),
        CompositeEffect(listOf(store("n", 2)) + pick(p) + store("other", 0)),
        gainN,
    ))
    val liveSilent = artifact("Twin live cursor silent", liveCursor(false))
    val liveAsks = artifact("Twin live cursor asks", liveCursor(true))
    val legacyReference = artifact("Twin legacy reference", CompositeEffect(shadowFollowed(false).effects))
    val chooseType = artifact("Twin string lists", CompositeEffect(listOf(
        CompositeEffect(listOf(EachPlayerChoosesCreatureTypeEffect("types"), store("other", 0))),
        ifHandHas(CardPredicate.HasSubtypeInStoredList("types")),
    )))

    // "Can't draw cards", and a draw-up-to whose not-drawn count a sibling reads.
    val drawLock = card("Twin draw lock") {
        typeLine = "Artifact"
        staticAbility { ability = RestrictCardsDrawnPerTurn(Player.Each, maxPerTurn = 0) }
    }
    val nothingToDraw = artifact("Twin nothing to draw", CompositeEffect(listOf(
        DrawUpToEffect(maxCards = 2, target = EffectTarget.Controller, storeNotDrawnAs = "n"), gainN,
    )))

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + cards + chooseType + legacyReference + depthRoot + depthNested + depthAfterPause + liveSilent + liveAsks +
            drawLock + nothingToDraw)
        initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Everything an outcome can differ in: life totals and where every card is. */
    fun GameTestDriver.outcome(): Map<String, Any?> = mapOf(
        "life" to listOf(player1, player2).map { getLifeTotal(it) },
        "hand" to getHand(player1).map { getCardName(it) }.sortedBy { it },
        "graveyard" to state.getZone(player1, Zone.GRAVEYARD).map { getCardName(it) },
        "stack" to state.stack.size,
        "frames" to state.continuationStack.size,
        "question" to (state.pendingDecision != null),
    )

    /**
     * Answer every question the same way: pick the bear, say yes, choose Bear, secretly choose 1.
     * Returns whether a card selection was asked.
     */
    fun GameTestDriver.answerAll(bear: EntityId): Boolean {
        var selectionAsked = false
        repeat(10) {
            when (val q = state.pendingDecision ?: return selectionAsked) {
                is SelectCardsDecision -> {
                    selectionAsked = true
                    submitDecision(q.playerId, CardsSelectedResponse(q.id, listOf(bear)))
                }
                is YesNoDecision -> submitDecision(q.playerId, YesNoResponse(q.id, true))
                is ChooseOptionDecision -> submitDecision(q.playerId, OptionChosenResponse(q.id, q.options.indexOf("Bear")))
                else -> submitDecision(q.playerId, NumberChosenResponse(q.id, 1))
            }
        }
        return selectionAsked
    }

    fun run(
        definition: CardDefinition,
        setupBoard: (GameTestDriver) -> Unit = {},
        whilePaused: (GameTestDriver) -> Unit = {},
    ): Pair<Map<String, Any?>, Boolean> {
        val d = setup()
        val player = d.player1
        val bear = d.putCardInHand(player, "Grizzly Bears")
        setupBoard(d)
        val artifact = d.putPermanentOnBattlefield(player, definition.name)
        d.submit(ActivateAbility(player, artifact, definition.script.activatedAbilities.single().id)).error shouldBe null
        d.state.stack.single { d.state.getEntity(it)?.has<ActivatedAbilityOnStackComponent>() == true }
        d.bothPass()
        if (d.state.pendingDecision != null) whilePaused(d)
        val asked = d.answerAll(bear)
        return d.outcome() to asked
    }

    for (shape in shapes) {
        test("${shape.name}: a paused run ends where the unpaused one does") {
            val (silent, silentAsked) = run(cards.single { it.name == "Twin ${shape.name} silent" })
            val (asked, askedAsked) = run(cards.single { it.name == "Twin ${shape.name} asks" })
            // The silent run never asked for the card; the other paused there and resumed.
            silentAsked shouldBe false
            askedAsked shouldBe true
            silent["life"] shouldBe listOf(shape.expectedLife, 20)
            asked shouldBe silent
        }
    }

    fun mayDepth(definition: CardDefinition): Int {
        val d = setup()
        val player = d.player1
        val bear = d.putCardInHand(player, "Grizzly Bears")
        val artifact = d.putPermanentOnBattlefield(player, definition.name)
        d.submit(ActivateAbility(player, artifact, definition.script.activatedAbilities.single().id)).error shouldBe null
        d.bothPass()
        (d.state.pendingDecision as? SelectCardsDecision)?.let { d.submitDecision(it.playerId, CardsSelectedResponse(it.id, listOf(bear))) }
        val answer = (d.state.continuationStack.last() as com.wingedsheep.engine.core.Suspension).answer
        return (answer as com.wingedsheep.engine.core.GatedEffectContinuation).effectContext.resolutionDepth
    }

    test("resolution depth: one deeper per nested composite, the same after a pause") {
        val root = mayDepth(depthRoot)
        val nested = mayDepth(depthNested)
        val afterPause = mayDepth(depthAfterPause)
        root shouldBe 1
        nested shouldBe root + 1
        afterPause shouldBe root
    }

    test("a paused live cursor survives encoding and decoding, and resumes like the unpaused run") {
        val persistence = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = false }
        val board: (GameTestDriver) -> Unit = { it.putCreatureOnBattlefield(it.player1, "Grizzly Bears") }
        val (reference, referenceAsked) = run(liveSilent, board)
        referenceAsked shouldBe false
        var checked = false
        val (resumed, asked) = run(liveAsks, board) { d ->
            val program = d.state.continuationStack.filterIsInstance<EffectProgram>().single()
            val nested = program.cursor.steps.map { it as com.wingedsheep.engine.core.CursorStep.Child }[1].delta.outputs
            nested.numbers shouldBe mapOf("n" to 1)
            nested.sacrificed.size shouldBe 1
            // (Exiling the source records no departure snapshot; the sacrifice records a snapshot.)
            nested.permits.isNotEmpty() shouldBe true
            val encoded = persistence.encodeToString(d.state)
            val decoded = persistence.decodeFromString<GameState>(encoded)
            decoded shouldBe d.state
            d.replaceState(decoded)
            checked = true
        }
        checked shouldBe true
        asked shouldBe true
        reference["life"] shouldBe listOf(21, 20)
        resumed shouldBe reference
    }

    test("a may branch's move keeps a keyless source followable, remembered yes or prompted yes") {
        // Composite[May(MoveToZone(Self, GRAVEYARD)), MoveToZone(Self, EXILE)] run under a captured
        // source reference with no resolution key, so only the hand-off can carry the move's permit.
        val identity = com.wingedsheep.sdk.scripting.AbilityIdentity(
            "Twin keyless", com.wingedsheep.sdk.scripting.AbilityId("Twin keyless:1"))
        val effect = CompositeEffect(listOf(
            Effects.May(com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD)),
            Effects.Exile(EffectTarget.Self),
        ))
        fun keyless(remembered: Boolean): Pair<Zone?, Boolean> {
            val d = setup()
            val player = d.player1
            val source = d.putCreatureOnBattlefield(player, "Grizzly Bears")
            val ref = d.state.objectRef(source)!!
            val context = EffectContext(
                sourceId = source, controllerId = player, abilityIdentity = identity,
                objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                    captured = true, origin = ref, source = ref, resolutionKey = null),
            )
            var state = d.state.pushContinuation(EffectProgram.queued(listOf(effect), context,
                origin = com.wingedsheep.engine.core.ProgramOrigin.BuiltBy("Test", null)))
            if (remembered) state = state.withYield(player, identity, com.wingedsheep.engine.state.YieldKind.ALWAYS_ANSWER_YES)
            d.replaceState(ContinuationHandler(d.services).drainAutomatic(state, emptyList()).state)
            val prompted = d.state.pendingDecision is YesNoDecision
            d.answerAll(source)
            val zone = listOf(Zone.BATTLEFIELD, Zone.GRAVEYARD, Zone.EXILE).firstOrNull { zone ->
                source in (if (zone == Zone.BATTLEFIELD) d.state.getBattlefield() else d.state.getZone(player, zone))
            }
            return zone to prompted
        }
        val (rememberedZone, rememberedPrompted) = keyless(remembered = true)
        val (promptedZone, promptedPrompted) = keyless(remembered = false)
        rememberedPrompted shouldBe false
        promptedPrompted shouldBe true
        rememberedZone shouldBe Zone.EXILE
        promptedZone shouldBe rememberedZone
    }

    test("a cost-then-payoff composite abandoned after a pause keeps the moves made before the pause") {
        // Composite[Composite(stopOnError)[Exile(Self), pick, failing cost, gain 5], Self to hand],
        // under a captured source reference with no resolution key: only the abandoned composite's
        // hand-off can carry the exile's permit to the step after it.
        val identity = com.wingedsheep.sdk.scripting.AbilityIdentity(
            "Twin abandoned", com.wingedsheep.sdk.scripting.AbilityId("Twin abandoned:1"))
        fun effect(p: Boolean) = CompositeEffect(listOf(
            CompositeEffect(listOf(Effects.Exile(EffectTarget.Self)) + pick(p) + failing + Effects.GainLife(5), stopOnError = true),
            com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect(EffectTarget.Self, Zone.HAND),
        ))
        fun abandoned(p: Boolean): Triple<Zone?, Int, Boolean> {
            val d = setup()
            val player = d.player1
            val bear = d.putCardInHand(player, "Grizzly Bears")
            val source = d.putCreatureOnBattlefield(player, "Grizzly Bears")
            val ref = d.state.objectRef(source)!!
            val context = EffectContext(
                sourceId = source, controllerId = player, abilityIdentity = identity,
                objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                    captured = true, origin = ref, source = ref, resolutionKey = null),
            )
            val state = d.state.pushContinuation(EffectProgram.queued(listOf(effect(p)), context,
                origin = com.wingedsheep.engine.core.ProgramOrigin.BuiltBy("Test", null)))
            d.replaceState(ContinuationHandler(d.services).drainAutomatic(state, emptyList()).state)
            val asked = d.answerAll(bear)
            val zone = listOf(Zone.BATTLEFIELD, Zone.HAND, Zone.EXILE).firstOrNull { zone ->
                source in (if (zone == Zone.BATTLEFIELD) d.state.getBattlefield() else d.state.getZone(player, zone))
            }
            return Triple(zone, d.getLifeTotal(player), asked)
        }
        val (silentZone, silentLife, silentAsked) = abandoned(p = false)
        val (askedZone, askedLife, askedAsked) = abandoned(p = true)
        silentAsked shouldBe false
        askedAsked shouldBe true
        // The payoff was abandoned; the exiled source was still followed back to hand.
        silentLife shouldBe 20
        silentZone shouldBe Zone.HAND
        (askedZone to askedLife) shouldBe (silentZone to silentLife)
    }

    test("a draw-up-to that can't draw stores its not-drawn count for the step after it") {
        val (outcome, asked) = run(nothingToDraw, setupBoard = { it.putPermanentOnBattlefield(it.player1, drawLock.name) })
        asked shouldBe false
        // Both cards are "not drawn": gain 2.
        outcome["life"] shouldBe listOf(22, 20)
    }

    test("a string list chosen inside a composite is read after it") {
        val (outcome, _) = run(chooseType)
        outcome["life"] shouldBe listOf(24, 20)
    }

    test("a converted legacy frame resumes like the same effects run unpaused") {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = false }
        val remaining = shadowFollowed(true).effects
        val reference = run(legacyReference).first

        val d = setup()
        val player = d.player1
        val bear = d.putCardInHand(player, "Grizzly Bears")
        val context = EffectContext(sourceId = null, controllerId = player)
        val encoded = json.parseToJsonElement(json.encodeToString(d.state)).jsonObject
        val oldFrame = JsonObject(mapOf(
            "type" to JsonPrimitive("com.wingedsheep.engine.core.EffectContinuation"),
            "remainingEffects" to json.encodeToJsonElement(ListSerializer(Effect.serializer()), remaining),
            "effectContext" to json.encodeToJsonElement(EffectContext.serializer(), context),
        ))
        val legacy = json.decodeFromString<GameState>(JsonObject(encoded + ("continuationStack" to JsonArray(listOf(oldFrame)))).toString())
        legacy.continuationStack.single()::class shouldBe EffectProgram::class
        val drained = ContinuationHandler(d.services).drainAutomatic(legacy, emptyList())
        d.replaceState(drained.state)
        d.answerAll(bear)
        d.getLifeTotal(player) shouldBe reference["life"].let { (it as List<*>)[0] }
    }

    test("stacked converted legacy frames still pass an inner frame's saved values outward") {
        // Saved mid-resolution: an outer frame with "gain n" left, and above it an inner frame whose
        // context holds n = 3 (injected before the save) with one step left. The old runner handed
        // its whole frame to the frame beneath, so the outer gain read n = 3.
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = false }
        val d = setup()
        val player = d.player1
        fun oldFrame(remaining: List<Effect>, context: EffectContext) = JsonObject(mapOf(
            "type" to JsonPrimitive("com.wingedsheep.engine.core.EffectContinuation"),
            "remainingEffects" to json.encodeToJsonElement(ListSerializer(Effect.serializer()), remaining),
            "effectContext" to json.encodeToJsonElement(EffectContext.serializer(), context),
        ))
        val outer = oldFrame(listOf(gainN), EffectContext(sourceId = null, controllerId = player))
        val inner = oldFrame(listOf(store("other", 0)), EffectContext(sourceId = null, controllerId = player,
            pipeline = PipelineState.EMPTY.copy(storedNumbers = mapOf("n" to 3))))
        val encoded = json.parseToJsonElement(json.encodeToString(d.state)).jsonObject
        val legacy = json.decodeFromString<GameState>(JsonObject(encoded + ("continuationStack" to JsonArray(listOf(outer, inner)))).toString())
        d.replaceState(ContinuationHandler(d.services).drainAutomatic(legacy, emptyList()).state)
        d.getLifeTotal(player) shouldBe 23
        d.state.continuationStack.size shouldBe 0
    }
})
