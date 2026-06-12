package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.conditions.CastChoiceIs
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Locks in the generic cast-choice slot conditions [CastChoiceMade] / [CastChoiceIs] reading the
 * unified [CastChoicesComponent] off the source entity — the SDK readers behind mtgish's
 * `AColorWasChosen` and the slot-based replacement for `SourceChosenModeIs`.
 */
class CastChoiceConditionTest : FunSpec({

    val evaluator = ConditionEvaluator()
    val player = EntityId.generate()
    val permanent = EntityId.generate()

    fun stateWith(bag: CastChoicesComponent?): GameState {
        var container = ComponentContainer()
            .with(
                CardComponent(
                    cardDefinitionId = "Test Permanent",
                    name = "Test Permanent",
                    manaCost = ManaCost(emptyList()),
                    typeLine = TypeLine(cardTypes = setOf(CardType.ARTIFACT)),
                    ownerId = player,
                )
            )
            .with(OwnerComponent(player))
            .with(ControllerComponent(player))
        if (bag != null) container = container.with(bag)
        return GameState(turnOrder = listOf(player))
            .withEntity(player, ComponentContainer())
            .withEntity(permanent, container)
            .addToZone(ZoneKey(player, Zone.BATTLEFIELD), permanent)
    }

    fun ctx() = EffectContext(sourceId = permanent, controllerId = player)

    test("CastChoiceMade is true only when the slot holds a value") {
        val bag = CastChoicesComponent(
            chosen = mapOf(ChoiceSlot.COLOR to ChoiceValue.ColorChoice(Color.RED))
        )
        val state = stateWith(bag)
        evaluator.evaluate(state, CastChoiceMade(ChoiceSlot.COLOR), ctx()) shouldBe true
        evaluator.evaluate(state, CastChoiceMade(ChoiceSlot.CREATURE_TYPE), ctx()) shouldBe false
        evaluator.evaluate(stateWith(null), CastChoiceMade(ChoiceSlot.COLOR), ctx()) shouldBe false
    }

    test("CastChoiceIs matches the stored value by text (color by enum name, type verbatim)") {
        val bag = CastChoicesComponent(
            chosen = mapOf(
                ChoiceSlot.COLOR to ChoiceValue.ColorChoice(Color.RED),
                ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"),
                ChoiceSlot.MODE to ChoiceValue.TextChoice("Khans")
            )
        )
        val state = stateWith(bag)
        evaluator.evaluate(state, CastChoiceIs(ChoiceSlot.COLOR, "RED"), ctx()) shouldBe true
        evaluator.evaluate(state, CastChoiceIs(ChoiceSlot.COLOR, "red"), ctx()) shouldBe true
        evaluator.evaluate(state, CastChoiceIs(ChoiceSlot.COLOR, "BLUE"), ctx()) shouldBe false
        evaluator.evaluate(state, CastChoiceIs(ChoiceSlot.CREATURE_TYPE, "Goblin"), ctx()) shouldBe true
        evaluator.evaluate(state, CastChoiceIs(ChoiceSlot.MODE, "Khans"), ctx()) shouldBe true
        evaluator.evaluate(state, CastChoiceIs(ChoiceSlot.MODE, "Dragons"), ctx()) shouldBe false
        // Slot with no value never matches.
        evaluator.evaluate(state, CastChoiceIs(ChoiceSlot.LAND_TYPE, "Forest"), ctx()) shouldBe false
    }
})
