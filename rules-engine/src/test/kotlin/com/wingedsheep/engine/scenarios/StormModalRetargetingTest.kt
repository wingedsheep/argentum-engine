package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.StormCopyModalTargetContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.StormCopyEffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phase 4 of `backlog/storm-implementation-correctness.md`: per 702.40a the copy
 * controller may choose new targets for each mode of a modal Storm spell. The
 * executor must pause once per target-requiring mode per copy and drop the
 * decision into a [StormCopyModalTargetContinuation] carrying the accumulated
 * per-mode targets.
 */
class StormModalRetargetingTest : FunSpec({

    fun buildState(
        p1: EntityId,
        p2: EntityId,
        spellEntity: EntityId,
        spellComponent: SpellOnStackComponent
    ): GameState {
        val card = CardComponent(
            cardDefinitionId = "Modal Storm",
            name = "Modal Storm",
            manaCost = ManaCost.parse("{1}{R}"),
            typeLine = TypeLine.instant(),
            oracleText = "",
            ownerId = p1,
            spellEffect = null
        )
        return GameState(
            activePlayerId = p1,
            priorityPlayerId = p1,
            turnOrder = listOf(p1, p2)
        )
            .withEntity(p1, ComponentContainer.of(PlayerComponent("P1"), LifeTotalComponent(20)))
            .withEntity(p2, ComponentContainer.of(PlayerComponent("P2"), LifeTotalComponent(20)))
            .withEntity(spellEntity, ComponentContainer.of(
                card,
                OwnerComponent(p1),
                ControllerComponent(p1),
                spellComponent
            ))
            .copy(stack = listOf(spellEntity))
    }

    test("modal source with a target-requiring mode pauses the executor with a ChooseTargetsDecision") {
        val p1 = EntityId.generate()
        val p2 = EntityId.generate()
        val spellEntity = EntityId.generate()

        // Mode 0 targets any; mode 1 has no targets. Original targeted p2.
        val source = SpellOnStackComponent(
            casterId = p1,
            chosenModes = listOf(0, 1),
            modeTargetsOrdered = listOf(
                listOf(ChosenTarget.Player(p2)),
                emptyList()
            ),
            modeTargetRequirements = mapOf(
                0 to listOf(AnyTarget()),
                1 to emptyList()
            )
        )

        val executor = StormCopyEffectExecutor(
            cardRegistry = CardRegistry(),
            targetFinder = TargetFinder()
        )
        val result = executor.execute(
            buildState(p1, p2, spellEntity, source),
            StormCopyEffect(
                copyCount = 1,
                spellEffect = DrawCardsEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                spellName = "Modal Storm"
            ),
            EffectContext(sourceId = spellEntity, controllerId = p1, opponentId = null)
        )

        result.isPaused shouldBe true
        val decision = result.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe p1
        decision.targetRequirements.size shouldBe 1

        val frame = result.state.continuationStack.last()
        frame.shouldBeInstanceOf<StormCopyModalTargetContinuation>()
        frame.remainingCopies shouldBe 1
        frame.totalCopies shouldBe 1
        frame.currentOrdinal shouldBe 0
        frame.accumulatedOrdinalTargets shouldBe emptyList()
        frame.chosenModes shouldBe listOf(0, 1)
    }

    test("modal source with no target-requiring mode inherits verbatim and lands the copy on the stack") {
        val p1 = EntityId.generate()
        val p2 = EntityId.generate()
        val spellEntity = EntityId.generate()

        val source = SpellOnStackComponent(
            casterId = p1,
            chosenModes = listOf(0, 1),
            modeTargetsOrdered = listOf(emptyList(), emptyList()),
            modeTargetRequirements = mapOf(0 to emptyList(), 1 to emptyList())
        )

        val executor = StormCopyEffectExecutor(
            cardRegistry = CardRegistry(),
            targetFinder = TargetFinder()
        )
        val result = executor.execute(
            buildState(p1, p2, spellEntity, source),
            StormCopyEffect(
                copyCount = 1,
                spellEffect = DrawCardsEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                spellName = "Modal Storm"
            ),
            EffectContext(sourceId = spellEntity, controllerId = p1, opponentId = null)
        )

        result.isSuccess shouldBe true
        val copyId = result.state.stack.single { id ->
            val c = result.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        val copy = result.state.getEntity(copyId)!!.get<SpellOnStackComponent>()!!
        copy.chosenModes shouldBe listOf(0, 1)
    }
})
