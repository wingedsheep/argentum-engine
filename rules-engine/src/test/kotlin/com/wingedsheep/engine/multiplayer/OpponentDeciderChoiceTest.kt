package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `Chooser.Opponent` means one opponent decides, and the controller of the spell or ability
 * picks which one. The Comprehensive Rules state that for choices made as a spell is cast
 * (CR 601.7a) or an ability activated (CR 602.3a); resolution-time choices follow the same
 * principle and cards spell it out in their rulings — Curator of Destinies: "You decide which
 * opponent chooses the pile while resolving [its] last ability."
 *
 * Before this, every `Chooser.Opponent` step silently handed the decision to the *first*
 * opponent. These tests pin the shared behavior in
 * [com.wingedsheep.engine.handlers.effects.ChooserResolution]: an extra controller prompt when
 * there is a real choice, and no prompt at all when there is only one opponent.
 */
class OpponentDeciderChoiceTest : FunSpec({

    /** Registry with the full card pool, so gathered cards have real definitions. */
    fun registry(): CardRegistry = CardRegistry().apply { register(TestCards.all) }

    fun initGame(registry: CardRegistry, playerCount: Int): Pair<GameState, List<EntityId>> {
        val deck = Deck(cards = List(40) { "Mountain" })
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..playerCount).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds
    }

    /** A source permanent under [controller] to hang the resolving effect off. */
    fun GameState.withSource(controller: EntityId, sourceId: EntityId): GameState {
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = "Mountain",
                name = "Divvy Source",
                manaCost = ManaCost.parse("{4}{U}{U}"),
                typeLine = TypeLine.parse("Creature — Sphinx"),
                ownerId = controller
            ),
            ControllerComponent(controller)
        )
        return withEntity(sourceId, container).addToZone(ZoneKey(controller, Zone.BATTLEFIELD), sourceId)
    }

    /** Two cards from [owner]'s library, one per pile. */
    fun twoPiles(state: GameState, owner: EntityId): Pair<List<EntityId>, List<EntityId>> {
        val library = state.getZone(ZoneKey(owner, Zone.LIBRARY))
        return listOf(library[0]) to listOf(library[1])
    }

    test("multiplayer ChoosePile: the controller picks which opponent chooses a pile") {
        val registry = registry()
        val (initial, players) = initGame(registry, playerCount = 4)
        val sourceId = EntityId.generate()
        val state = initial.withSource(players[0], sourceId)
        val (pileA, pileB) = twoPiles(state, players[0])

        val effect = ChoosePileEffect(
            pileA = "faceUp",
            pileB = "faceDown",
            pileALabel = "Face-up pile",
            pileBLabel = "Face-down pile",
            chooser = Chooser.Opponent,
            storeChosenAs = "chosen",
            storeOtherAs = "other"
        )
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = players[0],
            pipeline = PipelineState(storedCollections = mapOf("faceUp" to pileA, "faceDown" to pileB))
        )

        // First pause: the CONTROLLER is asked which opponent decides, one option per opponent.
        val first = EffectExecutorRegistry(cardRegistry = registry).execute(state, effect, context)
        first.isPaused shouldBe true
        val deciderPick = first.state.pendingDecision
        deciderPick.shouldNotBeNull()
        deciderPick.shouldBeInstanceOf<ChooseOptionDecision>()
        deciderPick.playerId shouldBe players[0]
        deciderPick.options shouldContainExactly listOf("Player 2", "Player 3", "Player 4")

        // Pick the SECOND opponent (players[2]) — the pre-fix behavior always used players[1].
        val processor = ActionProcessor(registry)
        val afterPick = processor.process(
            first.state,
            SubmitDecision(players[0], OptionChosenResponse(deciderPick.id, optionIndex = 1))
        ).result

        // Second pause: the pile choice itself, now owned by the chosen opponent.
        val pileChoice = afterPick.newState.pendingDecision
        pileChoice.shouldNotBeNull()
        pileChoice.shouldBeInstanceOf<ChooseOptionDecision>()
        pileChoice.playerId shouldBe players[2]
        pileChoice.options shouldContainExactly listOf("Face-up pile", "Face-down pile")
    }

    test("two-player ChoosePile: the sole opponent is forced, with no extra prompt") {
        val registry = registry()
        val (initial, players) = initGame(registry, playerCount = 2)
        val sourceId = EntityId.generate()
        val state = initial.withSource(players[0], sourceId)
        val (pileA, pileB) = twoPiles(state, players[0])

        val effect = ChoosePileEffect(
            pileA = "faceUp",
            pileB = "faceDown",
            pileALabel = "Face-up pile",
            pileBLabel = "Face-down pile",
            chooser = Chooser.Opponent,
            storeChosenAs = "chosen",
            storeOtherAs = "other"
        )
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = players[0],
            pipeline = PipelineState(storedCollections = mapOf("faceUp" to pileA, "faceDown" to pileB))
        )

        val result = EffectExecutorRegistry(cardRegistry = registry).execute(state, effect, context)
        result.isPaused shouldBe true
        val decision = result.state.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        // Straight to the pile choice, owned by the only opponent — no decider prompt in between.
        decision.playerId shouldBe players[1]
        decision.options shouldContainExactly listOf("Face-up pile", "Face-down pile")
    }

    test("the pick is shared vocabulary: SelectFromCollection routes to the chosen opponent too") {
        val registry = registry()
        val (initial, players) = initGame(registry, playerCount = 3)
        val sourceId = EntityId.generate()
        val state = initial.withSource(players[0], sourceId)
        val cards = state.getZone(ZoneKey(players[0], Zone.LIBRARY)).take(3)

        val effect = SelectFromCollectionEffect(
            from = "looked",
            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
            chooser = Chooser.Opponent,
            storeSelected = "picked",
            storeRemainder = "rest",
            prompt = "Choose a card"
        )
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = players[0],
            pipeline = PipelineState(storedCollections = mapOf("looked" to cards))
        )

        val first = EffectExecutorRegistry(cardRegistry = registry).execute(state, effect, context)
        first.isPaused shouldBe true
        val deciderPick = first.state.pendingDecision
        deciderPick.shouldNotBeNull()
        deciderPick.shouldBeInstanceOf<ChooseOptionDecision>()
        deciderPick.playerId shouldBe players[0]
        deciderPick.options shouldContainExactly listOf("Player 2", "Player 3")

        val afterPick = ActionProcessor(registry).process(
            first.state,
            SubmitDecision(players[0], OptionChosenResponse(deciderPick.id, optionIndex = 1))
        ).result

        val selection = afterPick.newState.pendingDecision
        selection.shouldNotBeNull()
        selection.shouldBeInstanceOf<SelectCardsDecision>()
        selection.playerId shouldBe players[2]
    }

    test("each opponent-chooses step gets its own pick — the first is not reused") {
        val registry = registry()
        val (initial, players) = initGame(registry, playerCount = 3)
        val sourceId = EntityId.generate()
        val state = initial.withSource(players[0], sourceId)
        val cards = state.getZone(ZoneKey(players[0], Zone.LIBRARY)).take(4)

        // Two independent "an opponent chooses" selections in one composite.
        val step = { from: String, into: String ->
            SelectFromCollectionEffect(
                from = from,
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Opponent,
                storeSelected = into,
                storeRemainder = "${into}Rest",
                prompt = "Choose a card"
            )
        }
        val composite = com.wingedsheep.sdk.scripting.effects.CompositeEffect(
            listOf(step("first", "firstPick"), step("second", "secondPick"))
        )
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = players[0],
            pipeline = PipelineState(
                storedCollections = mapOf("first" to cards.take(2), "second" to cards.drop(2))
            )
        )

        val processor = ActionProcessor(registry)
        val first = EffectExecutorRegistry(cardRegistry = registry).execute(state, composite, context)

        // Step 1: decider pick, then the selection itself.
        val pick1 = first.state.pendingDecision
        pick1.shouldNotBeNull()
        pick1.shouldBeInstanceOf<ChooseOptionDecision>()
        pick1.playerId shouldBe players[0]
        var current = processor.process(
            first.state,
            SubmitDecision(players[0], OptionChosenResponse(pick1.id, optionIndex = 0))
        ).result.newState

        val selection1 = current.pendingDecision
        selection1.shouldNotBeNull()
        selection1.shouldBeInstanceOf<SelectCardsDecision>()
        selection1.playerId shouldBe players[1]
        current = processor.process(
            current,
            SubmitDecision(
                players[1],
                com.wingedsheep.engine.core.CardsSelectedResponse(selection1.id, listOf(cards[0]))
            )
        ).result.newState

        // Step 2 must ask the controller again rather than reusing opponent 1.
        val pick2 = current.pendingDecision
        pick2.shouldNotBeNull()
        pick2.shouldBeInstanceOf<ChooseOptionDecision>()
        pick2.playerId shouldBe players[0]
        pick2.options shouldContainExactly listOf("Player 2", "Player 3")

        // And a different opponent can be named for it.
        current = processor.process(
            current,
            SubmitDecision(players[0], OptionChosenResponse(pick2.id, optionIndex = 1))
        ).result.newState
        val selection2 = current.pendingDecision
        selection2.shouldNotBeNull()
        selection2.shouldBeInstanceOf<SelectCardsDecision>()
        selection2.playerId shouldBe players[2]
    }
})
