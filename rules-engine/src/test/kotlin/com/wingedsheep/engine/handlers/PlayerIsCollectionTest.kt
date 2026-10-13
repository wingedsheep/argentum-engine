package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.PlayerIs
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlayerIsCollectionTest : FunSpec({
    val evaluator = PredicateEvaluator(cardRegistry = null).conditions
    val a = EntityId.generate()
    val b = EntityId.generate()
    val c = EntityId.generate()
    val state = GameState(turnOrder = listOf(a, b, c))
    val condition = PlayerIs(subject = Player.You, player = Player.InCollection("players"))
    fun context(player: EntityId, members: List<EntityId>) = EffectContext(null, player,
        pipeline = PipelineState(storedCollections = mapOf("players" to members)))

    test("matches any member of a saved player collection") {
        evaluator.evaluate(state, condition, context(a, listOf(a, b))) shouldBe true
        evaluator.evaluate(state, condition, context(b, listOf(a, b))) shouldBe true
        evaluator.evaluate(state, condition, context(c, listOf(a, b))) shouldBe false
    }
    test("missing empty collections and unresolved subjects fail closed") {
        evaluator.evaluate(state, condition, context(a, emptyList())) shouldBe false
        evaluator.evaluate(state, condition, EffectContext(null, a)) shouldBe false
        evaluator.evaluate(state, PlayerIs(subject = Player.TriggeringPlayer, player = Player.InCollection("players")),
            context(a, listOf(a, b))) shouldBe false
    }
    test("collection membership is resolution-only") {
        evaluator.evaluate(state, condition, ConditionEvaluationContext.Projection(
            sourceId = EntityId.generate(), sourceValues = null, projectedValues = emptyMap(), fallbackControllerId = a
        )) shouldBe false
    }
    test("ordinary player references retain equality semantics") {
        evaluator.evaluate(state, PlayerIs(subject = Player.You, player = Player.You), context(a, emptyList())) shouldBe true
        evaluator.evaluate(state, PlayerIs(subject = Player.You, player = Player.TriggeringPlayer),
            context(a, listOf(a, b)).copy(triggeringPlayerId = b)) shouldBe false
    }
})
