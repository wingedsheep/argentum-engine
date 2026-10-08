package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CastRecordComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.contextScopedReferenceIn
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine wiring for `DynamicAmount.ManaOfColorSpent` — "the amount of {U} spent to cast this",
 * the base of The Brothers' War's "for each {U}{U} spent" (Bladecoil Serpent, Clay Champion; their
 * scenario tests cover the cast-and-enter path end to end). Pinned here: each zone's payment record
 * is read, one color never leaks into another, a cast trigger's own snapshot wins over the entity,
 * an uncast object reads 0, and the projector does not treat the amount as context-scoped.
 */
class ManaOfColorSpentTest : FunSpec({

    val amounts = PredicateEvaluator(cardRegistry = null).amounts
    val player = EntityId.generate()

    fun GameState.spent(id: EntityId, color: Color, context: EffectContext = EffectContext(sourceId = id, controllerId = player)) =
        amounts.evaluate(this, DynamicAmounts.manaOfColorSpent(color), context)

    fun stateWith(id: EntityId, container: ComponentContainer): GameState =
        GameState().withEntity(player, ComponentContainer()).withEntity(id, container)

    test("a spell on the stack reads its live payment, color by color") {
        val id = EntityId.generate()
        val state = stateWith(
            id,
            ComponentContainer().with(
                SpellOnStackComponent(casterId = player, manaSpentBlue = 3, manaSpentRed = 2, manaSpentColorless = 4)
            )
        )
        state.spent(id, Color.BLUE) shouldBe 3
        state.spent(id, Color.RED) shouldBe 2
        withClue("colorless is not a color") { state.spent(id, Color.WHITE) shouldBe 0 }
    }

    test("a resolved permanent reads its cast record") {
        val id = EntityId.generate()
        val state = stateWith(id, ComponentContainer().with(CastRecordComponent(greenSpent = 5, whiteSpent = 1)))
        state.spent(id, Color.GREEN) shouldBe 5
        state.spent(id, Color.WHITE) shouldBe 1
        state.spent(id, Color.BLACK) shouldBe 0
    }

    test("pairs round down: five {G} is two {G}{G}") {
        val id = EntityId.generate()
        val state = stateWith(id, ComponentContainer().with(CastRecordComponent(greenSpent = 5)))
        amounts.evaluate(
            state,
            DynamicAmounts.manaOfColorSpent(Color.GREEN) / 2,
            EffectContext(sourceId = id, controllerId = player)
        ) shouldBe 2
    }

    test("an object that was never cast spent nothing") {
        val id = EntityId.generate()
        stateWith(id, ComponentContainer()).spent(id, Color.BLUE) shouldBe 0
    }

    test("a cast trigger's own payment snapshot wins over the entity's record") {
        val id = EntityId.generate()
        val state = stateWith(id, ComponentContainer().with(CastRecordComponent(blueSpent = 1)))
        val context = EffectContext(
            sourceId = id,
            controllerId = player,
            triggerContext = TriggerContext(triggeringEntityId = id, selfCastManaSpent = CastRecordComponent(blueSpent = 4))
        )
        state.spent(id, Color.BLUE, context) shouldBe 4
    }

    test("it reads the source entity, so the projector need not treat it as context-scoped") {
        contextScopedReferenceIn(DynamicAmounts.manaOfColorSpent(Color.BLUE)) shouldBe null
    }
})
