package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.MonarchChangedEvent
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.PlayerIsMonarch
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The monarch in a pod (CR 724): "an opponent is the monarch" must look at *every* opponent, and a
 * monarch who leaves the game hands the crown on at the same time (CR 724.4).
 */
class MonarchMultiplayerTest : FunSpec({

    val registry = CardRegistry().also { it.register(TestCards.all) }
    val conditions = PredicateEvaluator(registry).conditions

    fun boot(players: Int = 4, starting: Int = 0): Pair<GameState, List<EntityId>> {
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..players).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                startingPlayerIndex = starting,
                skipMulligans = true,
            )
        )
        return result.state to result.playerIds
    }

    fun GameState.holds(condition: Condition, controller: EntityId) =
        conditions.evaluate(this, condition, EffectContext(sourceId = null, controllerId = controller))

    test("'an opponent is the monarch' is existential over every opponent, not the first") {
        val (initial, p) = boot()
        val state = initial.copy(monarch = p[3])

        state.holds(Conditions.AnOpponentIsTheMonarch, p[0]) shouldBe true
        state.holds(PlayerIsMonarch(Player.EachOpponent), p[0]) shouldBe true
        state.holds(Conditions.YouAreTheMonarch, p[0]) shouldBe false
        state.holds(Conditions.YouAreTheMonarch, p[3]) shouldBe true
        state.holds(Conditions.AnOpponentIsTheMonarch, p[3]) shouldBe false
    }

    test("with no monarch, both conditions are false and 'not the monarch' is true (CR 724.1)") {
        val (state, p) = boot()
        state.monarch shouldBe null
        state.holds(Conditions.YouAreTheMonarch, p[0]) shouldBe false
        state.holds(Conditions.AnOpponentIsTheMonarch, p[0]) shouldBe false
        state.holds(NotCondition(Conditions.YouAreTheMonarch), p[0]) shouldBe true
    }

    test("a monarch who leaves hands the crown to the active player (CR 724.4)") {
        val (initial, p) = boot()
        initial.activePlayerId shouldBe p[0]
        val processor = ActionProcessor(registry)

        val result = processor.process(initial.copy(monarch = p[2]), Concede(p[2])).result
        val state = result.newState

        state.gameOver shouldBe false
        state.monarch shouldBe p[0]
        result.events.filterIsInstance<MonarchChangedEvent>().single().let {
            it.previousMonarchId shouldBe p[2]
            it.newMonarchId shouldBe p[0]
        }
    }

    test("an active monarch who leaves hands the crown to the next player in turn order (CR 724.4)") {
        val (initial, p) = boot(starting = 1)
        initial.activePlayerId shouldBe p[1]
        val processor = ActionProcessor(registry)

        val state = processor.process(initial.copy(monarch = p[1]), Concede(p[1])).result.newState

        state.gameOver shouldBe false
        state.monarch shouldBe p[2]
    }

    test("a non-monarch leaving doesn't move the crown") {
        val (initial, p) = boot()
        val processor = ActionProcessor(registry)

        val result = processor.process(initial.copy(monarch = p[3]), Concede(p[1])).result

        result.newState.monarch shouldBe p[3]
        result.events.filterIsInstance<MonarchChangedEvent>() shouldBe emptyList()
    }
})
