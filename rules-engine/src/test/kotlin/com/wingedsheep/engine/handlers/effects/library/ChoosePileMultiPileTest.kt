package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.serialization.CardSerialization
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [ChoosePileEffect] beyond two piles ([ChoosePileEffect.additionalPiles]) and at random
 * ([ChoosePileEffect.atRandom]) — Whims of the Fates: "separates all permanents they control into
 * three piles. Then each player chooses one of their piles at random" (ruling: "each pile must
 * have an equal chance of being chosen", empty piles included).
 */
class ChoosePileMultiPileTest : FunSpec({

    val registry = CardRegistry().apply { register(TestCards.all) }

    fun initGame(): Pair<GameState, EntityId> {
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..2).map { PlayerConfig("Player $it", Deck(cards = List(40) { "Mountain" }), 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds[0]
    }

    fun threePiles(state: GameState, owner: EntityId): Map<String, List<EntityId>> {
        val library = state.getZone(ZoneKey(owner, Zone.LIBRARY))
        return mapOf("p1" to listOf(library[0]), "p2" to listOf(library[1], library[2]), "p3" to emptyList())
    }

    fun effect(atRandom: Boolean) = ChoosePileEffect(
        pileA = "p1",
        pileB = "p2",
        additionalPiles = listOf("p3"),
        storeChosenAs = "chosen",
        storeOtherAs = "other",
        atRandom = atRandom
    )

    test("a player choosing among three piles sees one option per pile, with its cards") {
        val (state, controller) = initGame()
        val piles = threePiles(state, controller)
        val context = EffectContext(sourceId = null, controllerId = controller, pipeline = PipelineState(storedCollections = piles))

        val result = EngineServices(registry).effectExecutorRegistry.execute(state, effect(atRandom = false), context)
        (result.outcome is Outcome.Paused) shouldBe true
        val decision = result.state.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options shouldContainExactly listOf("Pile 1", "Pile 2", "Pile 3")
        decision.optionCardIds shouldBe mapOf(0 to piles.getValue("p1"), 1 to piles.getValue("p2"), 2 to emptyList())

        // Choosing the third pile (empty) routes it to chosen; the other two merge into other.
        val after = ActionProcessor(registry).process(
            result.state, SubmitDecision(controller, OptionChosenResponse(decision.id, optionIndex = 2))
        ).result
        after.newState.pendingDecision.shouldBeNull()
    }

    test("the resumer routes the chosen pile and merges every other pile") {
        val piles = listOf(listOf(EntityId.generate()), listOf(EntityId.generate(), EntityId.generate()), emptyList())
        val routed = ChoosePileRouting.route(piles, chosenIndex = 1, storeChosenAs = "chosen", storeOtherAs = "other")
        routed.getValue("chosen") shouldContainExactly piles[1]
        routed.getValue("other") shouldContainExactly piles[0]
        ChoosePileRouting.route(piles, chosenIndex = 2, "chosen", "other").getValue("other") shouldContainExactly piles[0] + piles[1]
    }

    test("at random: no decision, and every pile — the empty one too — can be the pick") {
        val (initial, controller) = initGame()
        val piles = threePiles(initial, controller)
        val context = EffectContext(sourceId = null, controllerId = controller, pipeline = PipelineState(storedCollections = piles))
        val seen = mutableSetOf<List<EntityId>>()
        for (seed in 1L..60L) {
            val state = initial.copy(rng = GameRng.seeded(seed))
            val result = EngineServices(registry).effectExecutorRegistry.execute(state, effect(atRandom = true), context)
            result.state.pendingDecision.shouldBeNull()
            val chosen = result.updatedCollections.getValue("chosen")
            val other = result.updatedCollections.getValue("other")
            (chosen in piles.values) shouldBe true
            (chosen + other) shouldContainExactlyInAnyOrder piles.values.flatten()
            seen += chosen
        }
        seen shouldContainExactlyInAnyOrder piles.values.toList()
    }

    test("two-pile ChoosePile serializes exactly as before (new axes default off)") {
        val encoded = CardSerialization.compactJson.encodeToString(Effect.serializer(), ChoosePileEffect(pileA = "a", pileB = "b", storeChosenAs = "c", storeOtherAs = "o"))
        encoded.contains("additionalPile") shouldBe false
        encoded.contains("atRandom") shouldBe false
    }

    test("a label is required for every additional pile") {
        shouldThrow<IllegalArgumentException> {
            ChoosePileEffect(pileA = "a", pileB = "b", storeChosenAs = "c", storeOtherAs = "o",
                additionalPiles = listOf("x", "y"), additionalPileLabels = listOf("only one"))
        }
    }
})
