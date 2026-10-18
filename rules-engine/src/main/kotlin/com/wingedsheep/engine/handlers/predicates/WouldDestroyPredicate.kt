package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.handlers.CapturedObjectBinding
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor
import com.wingedsheep.engine.mechanics.stack.resolvingSpellEffect
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The battlefield permanents the spell or ability [stackObjectId] would destroy if it resolved
 * right now — the read behind [com.wingedsheep.sdk.scripting.predicates.CardPredicate.WouldDestroy]
 * ("counter target spell if it would destroy a land you control", Equinox).
 *
 * A static preview of the object's effect tree against the current state, built from the same
 * context resolution would use (its controller, chosen targets, chosen modes, X). It follows only
 * what the text does directly and unconditionally (the Equinox rulings):
 *  - a destroy aimed at a target or a bound object (`MoveToZoneEffect(byDestruction)`);
 *  - a "destroy all/each …" sweep, as a group iteration or a gather-then-destroy pipeline;
 *  - sequences, per-target iteration and the modes chosen on cast.
 * Anything else contributes nothing: a "may", a condition, a choice made on resolution, a random
 * outcome, a sacrifice, or damage the game rules later turn into destruction.
 */
internal fun stackObjectWouldDestroy(
    state: GameState,
    stackObjectId: EntityId,
    evaluator: PredicateEvaluator,
    cardRegistry: CardRegistry?,
): Set<EntityId> {
    val container = state.getEntity(stackObjectId) ?: return emptySet()
    val targetsComponent = container.get<TargetsComponent>()
    val targets = targetsComponent?.targets.orEmpty()
    val requirements = targetsComponent?.targetRequirements.orEmpty()
    val namedTargets = EffectContext.buildNamedTargets(requirements, targets)

    val spell = container.get<SpellOnStackComponent>()
    val activated = container.get<ActivatedAbilityOnStackComponent>()
    val triggered = container.get<TriggeredAbilityOnStackComponent>()
    val (effect, context) = when {
        spell != null -> {
            val card = container.get<CardComponent>()
            val definition = card?.let { cardRegistry?.getCard(it.name) }
            val effect = resolvingSpellEffect(state, stackObjectId, spell, card, definition) ?: return emptySet()
            effect to EffectContext(
                sourceId = stackObjectId,
                controllerId = spell.casterId,
                targets = targets,
                alignedTargets = targets,
                xValue = spell.xValue,
                chosenModes = spell.chosenModes,
                modeTargetsOrdered = spell.modeTargetsOrdered,
                modeTargetRequirements = spell.modeTargetRequirements,
                pipeline = com.wingedsheep.engine.handlers.PipelineState(namedTargets = namedTargets),
            )
        }
        activated != null -> activated.effect to EffectContext(
            sourceId = activated.sourceId,
            controllerId = activated.controllerId,
            targets = targets,
            alignedTargets = targets,
            xValue = activated.xValue,
            chosenModes = activated.chosenModes,
            modeTargetsOrdered = activated.modeTargetsOrdered,
            modeTargetRequirements = activated.modeTargetRequirements,
            pipeline = com.wingedsheep.engine.handlers.PipelineState(namedTargets = namedTargets),
        )
        triggered != null -> triggered.effect to EffectContext.forTriggeredAbility(triggered, targets, requirements)
        else -> return emptySet()
    }
    return DestructionPreview(state, evaluator).apply { walk(effect, context, mutableMapOf()) }.destroyed
}

private class DestructionPreview(private val state: GameState, private val evaluator: PredicateEvaluator) {
    val destroyed = linkedSetOf<EntityId>()

    /** [gathered] holds the battlefield collections a preceding `GatherCardsEffect` named. */
    fun walk(effect: Effect, context: EffectContext, gathered: MutableMap<String, List<EntityId>>) {
        when (effect) {
            is CompositeEffect -> effect.effects.forEach { walk(it, context, gathered) }

            is MoveToZoneEffect -> if (effect.byDestruction && effect.destination == Zone.GRAVEYARD) {
                TargetResolutionUtils.resolveTarget(effect.target, context, state)
                    ?.takeIf { it in state.getBattlefield() }
                    ?.let(destroyed::add)
            }

            is GatherCardsEffect -> (effect.source as? CardSource.BattlefieldMatching)?.let { source ->
                gathered[effect.storeAs] = gatherBattlefield(source, context)
            }

            is MoveCollectionEffect -> {
                val toGraveyard = (effect.destination as? CardDestination.ToZone)?.zone == Zone.GRAVEYARD
                if (effect.moveType == MoveType.Destroy && toGraveyard) {
                    gathered[effect.from]?.let(destroyed::addAll)
                }
            }

            is ForEachEffect -> forEachContexts(effect.space, context).forEach { walk(effect.body, it, mutableMapOf()) }

            is ModalEffect -> if (context.chosenModes.isNotEmpty()) {
                ModalEffectExecutor.buildModeEntries(
                    effect, context.chosenModes, context.modeTargetsOrdered, context.modeTargetRequirements
                ).forEach { entry ->
                    val modeContext = context.copy(
                        targets = entry.targets,
                        alignedTargets = entry.targets,
                        pipeline = context.pipeline.copy(
                            namedTargets = EffectContext.buildNamedTargets(entry.targetRequirements, entry.targets)
                        ),
                    )
                    walk(entry.effect, modeContext, mutableMapOf())
                }
            }

            // A "may", a condition, a resolution-time choice or a random outcome: not something the
            // spell would do if it resolved right now, so it never counts (Equinox rulings).
            else -> Unit
        }
    }

    private fun gatherBattlefield(source: CardSource.BattlefieldMatching, context: EffectContext): List<EntityId> {
        val playerId = if (source.player != Player.Each) {
            TargetResolutionUtils.resolvePlayerRef(source.player, context, state) ?: context.controllerId
        } else null
        val filter = if (playerId != null) source.filter.youControl() else source.filter
        val predicateContext = PredicateContext.fromEffectContext(context)
            .let { if (playerId != null) it.copy(controllerId = playerId) else it }
        val chosenIds = if (source.excludeChosenTargets) context.targets.mapNotNull { it.entityIdOrNull() }.toSet()
        else emptySet()
        return BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter, predicateContext, if (source.excludeSelf) context.sourceId else null,
            predicateEvaluator = evaluator,
        ).filter { (!source.excludeTriggering || it != context.triggeringEntityId) && it !in chosenIds }
    }

    /** The per-iteration contexts of a `ForEachEffect`, bound the way its executor binds them. */
    private fun forEachContexts(space: IterationSpace, context: EffectContext): List<EffectContext> = when (space) {
        IterationSpace.Targets -> context.targets.map { target ->
            context.copy(targets = listOf(target), alignedTargets = listOf(target))
        }
        is IterationSpace.TargetsOf -> {
            val named = context.pipeline.namedTargets
            (named[space.group]?.let(::listOf)
                ?: named.entries.filter { it.key.startsWith("${space.group}[") }.sortedBy { it.key }.map { it.value })
                .mapNotNull { it.entityIdOrNull() }
                .map { bindIteration(context, it) }
        }
        is IterationSpace.Group -> {
            val filter = space.filter
            if (filter.chosenSubtypeKey != null) emptyList()
            else {
                val excludeTarget = if (filter.excludeTarget) context.targets.firstOrNull()?.entityIdOrNull() else null
                BattlefieldFilterUtils.findMatchingOnBattlefield(
                    state, filter.baseFilter, context, if (filter.excludeSelf) context.sourceId else null,
                    predicateEvaluator = evaluator,
                ).filter { it != excludeTarget && (!filter.excludeTriggeringEntity || it != context.triggeringEntityId) }
                    .map { bindIteration(context, it) }
            }
        }
        else -> emptyList()
    }

    private fun bindIteration(context: EffectContext, entityId: EntityId): EffectContext = context.copy(
        objectReferences = context.objectReferences.copy(
            iteration = CapturedObjectBinding(entityId, state.objectRef(entityId))
        ),
        iterationReferenceLost = false,
    )

    private fun ChosenTarget.entityIdOrNull(): EntityId? = when (this) {
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Card -> cardId
        is ChosenTarget.Spell -> spellEntityId
        is ChosenTarget.Player -> null
    }
}
