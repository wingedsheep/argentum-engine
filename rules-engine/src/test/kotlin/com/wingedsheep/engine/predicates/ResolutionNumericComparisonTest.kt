package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.stack.ResolutionTargetValidator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.matchers.shouldBe

class ResolutionNumericComparisonTest : ScenarioTestBase() {
    init {
        test("trigger target comparison preserves original source identity after a blink") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val source = game.findPermanent("Grizzly Bears")!!
            val target = ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)
            game.state = game.state.updateEntity(source) {
                it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))
            }
            val original = game.state.objectRef(source)!!
            val lastKnownPower = game.state.projectedState.getPower(source)!!
            val resolution = EffectContext(
                sourceId = source,
                controllerId = game.player1Id,
                targets = listOf(target),
                objectReferences = ObjectReferenceEnvironment(captured = true,
                    origin = original, source = original, triggering = original),
                triggerContext = TriggerContext(triggeringEntityId = source, lastKnownPower = lastKnownPower),
                triggeringEntityId = source,
            )
            game.state = game.zones.moveToZone(game.state, source, Zone.EXILE).state
            game.state = game.zones.moveToZone(game.state, source, Zone.BATTLEFIELD).state
            game.state.projectedState.getPower(source) shouldBe 2
            val requirement = TargetObject(filter = TargetFilter.Creature.compareNumericProperty(
                CardNumericProperty.TOUGHNESS, ComparisonOperator.LT,
                DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.Power),
            ))
            val validator = ResolutionTargetValidator(PredicateEvaluator(cardRegistry))
            validator.validateTargets(
                state = game.state,
                targets = listOf(target),
                controllerId = game.player1Id,
                targetRequirements = listOf(requirement),
                sourceId = source,
                targetingSourceType = TargetingSourceType.TRIGGERED_ABILITY,
                triggeringEntityId = source,
                resolution = resolution,
            ) shouldBe listOf(target)
        }
    }
}
