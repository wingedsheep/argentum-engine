package com.wingedsheep.engine.scenarios

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
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Phase 5 of `backlog/storm-implementation-correctness.md`: per rule 707.7b a
 * copy whose targets can't be legally replaced is still put on the stack — it
 * then fizzles on resolution per 608.2b / 112.3b. Previously the executor
 * short-circuited and silently skipped the copy when no legal targets existed.
 */
class StormIllegalTargetFizzleTest : FunSpec({

    fun buildState(
        p1: EntityId,
        p2: EntityId,
        spellEntity: EntityId,
        spellComponent: SpellOnStackComponent,
        targetsComponent: TargetsComponent?
    ): GameState {
        val card = CardComponent(
            cardDefinitionId = "Creature Bolt",
            name = "Creature Bolt",
            manaCost = ManaCost.parse("{R}"),
            typeLine = TypeLine.instant(),
            oracleText = "",
            ownerId = p1,
            spellEffect = null
        )
        var container = ComponentContainer.of(
            card,
            OwnerComponent(p1),
            ControllerComponent(p1),
            spellComponent
        )
        if (targetsComponent != null) {
            container = container.with(targetsComponent)
        }
        return GameState(
            activePlayerId = p1,
            priorityPlayerId = p1,
            turnOrder = listOf(p1, p2)
        )
            .withEntity(p1, ComponentContainer.of(PlayerComponent("P1"), LifeTotalComponent(20)))
            .withEntity(p2, ComponentContainer.of(PlayerComponent("P2"), LifeTotalComponent(20)))
            .withEntity(spellEntity, container)
            .copy(stack = listOf(spellEntity))
    }

    test("copy is still put on the stack when no legal target replacement exists") {
        val p1 = EntityId.generate()
        val p2 = EntityId.generate()
        val spellEntity = EntityId.generate()
        // A stale permanent id the source originally targeted — it no longer
        // exists on the battlefield, so validateTargets will fail at resolution.
        val deadCreature = EntityId.generate()

        val requirement = TargetObject(filter = TargetFilter(GameObjectFilter.Creature))
        val source = SpellOnStackComponent(casterId = p1)
        val sourceTargets = TargetsComponent(
            targets = listOf(ChosenTarget.Permanent(deadCreature)),
            targetRequirements = listOf(requirement)
        )

        val executor = StormCopyEffectExecutor(
            cardRegistry = CardRegistry(),
            targetFinder = TargetFinder()
        )
        val result = executor.execute(
            buildState(p1, p2, spellEntity, source, sourceTargets),
            StormCopyEffect(
                copyCount = 1,
                spellEffect = DealDamageEffect(1, EffectTarget.ContextTarget(0)),
                spellTargetRequirements = listOf(requirement),
                spellName = "Creature Bolt"
            ),
            EffectContext(sourceId = spellEntity, controllerId = p1, opponentId = null)
        )

        result.isSuccess shouldBe true

        // Copy is on the stack with CopyOfComponent
        val copyId = result.state.stack.single { id ->
            val c = result.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        // Copy inherits the source's (now illegal) targets so it will fizzle on resolution.
        val copyTargets = result.state.getEntity(copyId)!!.get<TargetsComponent>()!!
        copyTargets.targets shouldBe listOf(ChosenTarget.Permanent(deadCreature))
        copyTargets.targetRequirements shouldBe listOf(requirement)
    }

    test("all remaining copies are put on the stack when no legal target exists for any of them") {
        val p1 = EntityId.generate()
        val p2 = EntityId.generate()
        val spellEntity = EntityId.generate()
        val deadCreature = EntityId.generate()

        val requirement = TargetObject(filter = TargetFilter(GameObjectFilter.Creature))
        val source = SpellOnStackComponent(casterId = p1)
        val sourceTargets = TargetsComponent(
            targets = listOf(ChosenTarget.Permanent(deadCreature)),
            targetRequirements = listOf(requirement)
        )

        val executor = StormCopyEffectExecutor(
            cardRegistry = CardRegistry(),
            targetFinder = TargetFinder()
        )
        val result = executor.execute(
            buildState(p1, p2, spellEntity, source, sourceTargets),
            StormCopyEffect(
                copyCount = 3,
                spellEffect = DealDamageEffect(1, EffectTarget.ContextTarget(0)),
                spellTargetRequirements = listOf(requirement),
                spellName = "Creature Bolt"
            ),
            EffectContext(sourceId = spellEntity, controllerId = p1, opponentId = null)
        )

        result.isSuccess shouldBe true

        // Three copies on the stack alongside the source.
        val copyIds = result.state.stack.filter { id ->
            val c = result.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        copyIds.size shouldBe 3
    }
})
