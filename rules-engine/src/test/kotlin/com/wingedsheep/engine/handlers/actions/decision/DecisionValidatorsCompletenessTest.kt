package com.wingedsheep.engine.handlers.actions.decision

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class DecisionValidatorsCompletenessTest : FunSpec({

    val player = EntityId.of("player")
    val first = EntityId.of("first")
    val second = EntityId.of("second")
    val context = DecisionContext(phase = DecisionPhase.RESOLUTION)

    test("target response may omit an optional requirement but not a mandatory one") {
        val decision = ChooseTargetsDecision(
            id = "targets",
            playerId = player,
            prompt = "Choose targets",
            context = context,
            targetRequirements = listOf(
                TargetRequirementInfo(index = 0, description = "mandatory"),
                TargetRequirementInfo(index = 1, description = "optional", minTargets = 0),
            ),
            legalTargets = mapOf(0 to listOf(first), 1 to listOf(second)),
        )

        DecisionValidators.validate(
            decision,
            TargetsResponse(decision.id, mapOf(0 to listOf(first))),
        ).shouldBeNull()

        DecisionValidators.validate(
            decision,
            TargetsResponse(decision.id, emptyMap()),
        ).shouldNotBeNull()
    }

    test("ordering response contains every object exactly once") {
        val decision = OrderObjectsDecision(
            id = "order",
            playerId = player,
            prompt = "Order",
            context = context,
            objects = listOf(first),
        )

        DecisionValidators.validate(
            decision,
            OrderedResponse(decision.id, listOf(first)),
        ).shouldBeNull()

        DecisionValidators.validate(
            decision,
            OrderedResponse(decision.id, listOf(first, first)),
        ).shouldNotBeNull()
    }
})
