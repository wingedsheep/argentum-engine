package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.handlers.DependentTargetSelection
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.withCount

/**
 * Target legality belongs to the prospective copy, not the effect making it or the original.
 * The scratch entity is never put on the stack or returned to gameplay: new targets are chosen
 * before placement (CR 707.10c). In particular, the original keeps its own colors and remains a
 * possible target; a red copy can target something protected from the original's blue color.
 */
internal object SpellCopyTargets {

    data class Selection(val targets: List<ChosenTarget>, val retained: Set<Int>)

    fun originals(state: GameState, sourceId: EntityId, ordinal: Int? = null): List<ChosenTarget> =
        if (ordinal == null) state.spellOrLastKnown(sourceId)?.get<TargetsComponent>()?.targets.orEmpty()
        else state.spellOrLastKnown(sourceId)?.get<SpellOnStackComponent>()?.modeTargetsOrdered?.getOrNull(ordinal).orEmpty()

    fun selection(state: GameState, sourceId: EntityId, response: TargetsResponse, ordinal: Int? = null): Selection {
        val originals = originals(state, sourceId, ordinal)
        val retained = originals.indices.filterTo(mutableSetOf()) { response.selectedTargets[it].isNullOrEmpty() }
        return Selection(originals.mapIndexed { i, original ->
            response.selectedTargets[i]?.singleOrNull()?.let { entityIdToChosenTarget(state, it) } ?: original
        }, retained)
    }

    data class Prompt(val requirements: List<TargetRequirementInfo>, val legal: Map<Int, List<EntityId>>)

    fun prompt(state: GameState, finder: TargetFinder, sourceId: EntityId, controllerId: EntityId,
               requirements: List<TargetRequirement>, exceptions: CopyExceptions, ordinal: Int? = null): Prompt {
        val originals = originals(state, sourceId, ordinal)
        val aligned = alignedRequirements(requirements, originals.size)
        val tables = if (DependentTargetSelection.isRequired(requirements) && aligned.dropLast(1).all { it.count == 1 }) {
            preview(state, sourceId, controllerId, exceptions)?.let { dependentTables(it, finder, aligned, originals) }
        } else null
        val byRequirement = if (tables == null) legalTargets(state, finder, sourceId, controllerId, requirements, exceptions)
            else tables.mapIndexed { index, table -> index to table.values.flatten().distinct() }.toMap()
        val distribution = state.spellOrLastKnown(sourceId)?.get<SpellOnStackComponent>()?.damageDistribution.orEmpty()
        val infos = mutableListOf<TargetRequirementInfo>()
        val legal = mutableMapOf<Int, List<EntityId>>()
        var slot = 0
        for ((index, req) in aligned.withIndex()) {
            repeat(req.count) {
                val amount = distribution[originals[slot].entityId()]
                infos += TargetRequirementInfo(index = slot, minTargets = 0, maxTargets = 1,
                    description = "${req.description} — target ${slot + 1}" +
                        (amount?.let { ": $it to this target" } ?: ""), emptyChoiceLabel = "Keep original target",
                    legalTargetsByPrefix = tables?.get(index)?.takeIf { index > 0 },
                    prefixLength = index.takeIf { tables != null && index > 0 })
                legal[slot] = byRequirement[index].orEmpty()
                slot++
            }
        }
        return Prompt(infos, legal)
    }

    /**
     * Choices per requirement keyed by the earlier slots' answers, when a filter reads an earlier
     * target (The Fall of Kroog: "target land that player controls"). Every earlier slot is either
     * kept — its original target stays bound (CR 707.10c) — or changed to one of its own choices.
     * Every requirement before the last is a single slot here, so requirement and slot indices agree.
     * A changed choice is offered only when the later slots can still be answered, kept or changed,
     * without unseating a kept target (CR 115.7d) — no opponent without lands for The Fall of Kroog.
     */
    private fun dependentTables(
        preview: Preview,
        finder: TargetFinder,
        aligned: List<TargetRequirement>,
        originals: List<ChosenTarget>,
    ): List<Map<String, List<EntityId>>> {
        val tables = aligned.map { linkedMapOf<String, List<EntityId>>() }
        fun keepable(index: Int, prefix: List<ChosenTarget>) =
            !unseats(preview, finder, aligned, originals, index, prefix + originals.drop(prefix.size))
        fun visit(index: Int, keys: List<String>, prefix: List<ChosenTarget>): Boolean {
            val legal = preview.legal(finder, aligned[index], prefix, EffectContext.buildNamedTargets(aligned.take(index), prefix))
            val key = keys.joinToString(",")
            if (index == aligned.lastIndex) {
                tables[index][key] = legal
                return legal.isNotEmpty() || keepable(index, prefix)
            }
            val keptCompletes = visit(index + 1, keys + "", prefix + originals[index]) && keepable(index, prefix)
            val completing = legal.filter { visit(index + 1, keys + it.value, prefix + entityIdToChosenTarget(preview.state, it)) }
            tables[index][key] = completing
            return keptCompletes || completing.isNotEmpty()
        }
        visit(0, emptyList(), emptyList())
        return tables
    }

    /** The prospective copy, on a scratch state, so legality reads its colors and controller. */
    private class Preview(val state: GameState, val copyId: EntityId, val card: CardComponent,
                          val controllerId: EntityId, val xValue: Int?) {
        val subtypes: Set<String> = card.typeLine.subtypes.mapTo(mutableSetOf()) { it.value }

        fun legal(finder: TargetFinder, requirement: TargetRequirement, prefix: List<ChosenTarget> = emptyList(),
                  named: Map<String, ChosenTarget> = emptyMap()): List<EntityId> =
            finder.findLegalTargets(state, requirement, controllerId, copyId, targetingSourceType = TargetingSourceType.SPELL,
                pipelineContext = if (prefix.isEmpty()) null else PredicateContext(controllerId = controllerId,
                    sourceId = copyId, xValue = xValue, targets = prefix, namedTargets = named),
            ).filter { candidate ->
                finder.validator.validateSingleTarget(
                    state = state,
                    target = entityIdToChosenTarget(state, candidate),
                    requirement = requirement,
                    casterId = controllerId,
                    sourceColors = card.colors,
                    sourceSubtypes = subtypes,
                    sourceId = copyId,
                    xValue = xValue,
                    allTargets = prefix,
                    targetingSourceType = TargetingSourceType.SPELL,
                    namedTargets = named,
                ) == null
            }
    }

    private fun preview(state: GameState, sourceId: EntityId, controllerId: EntityId, exceptions: CopyExceptions): Preview? {
        val source = state.spellOrLastKnown(sourceId) ?: return null
        val card = source.get<CardComponent>() ?: return null
        val spell = source.get<SpellOnStackComponent>() ?: return null
        val (copyId, scratch) = state.newEntity()
        val copied = CopyExceptionApplier.apply(card, exceptions).copy(ownerId = controllerId)
        return Preview(scratch.withEntity(copyId, ComponentContainer.of(copied, spell.copy(casterId = controllerId))),
            copyId, copied, controllerId, spell.xValue)
    }

    fun validate(state: GameState, finder: TargetFinder, answer: AnswerContinuation, response: TargetsResponse): String? {
        val sourceId: EntityId
        val controller: EntityId
        val requirements: List<TargetRequirement>
        val exceptions: CopyExceptions
        val ordinal: Int?
        when (answer) {
            is StormCopyTargetContinuation -> {
                sourceId = answer.sourceId; controller = answer.controllerId
                requirements = answer.spellTargetRequirements; exceptions = answer.exceptions; ordinal = null
            }
            is StormCopyModalTargetContinuation -> {
                sourceId = answer.sourceId; controller = answer.controllerId
                requirements = answer.modeTargetRequirements[answer.chosenModes[answer.currentOrdinal]].orEmpty()
                exceptions = answer.exceptions; ordinal = answer.currentOrdinal
            }
            else -> return null
        }
        val selection = selection(state, sourceId, response, ordinal)
        if (selection.retained.size == selection.targets.size) return null
        val preview = preview(state, sourceId, controller, exceptions) ?: return "Source spell missing"
        val aligned = alignedRequirements(requirements, selection.targets.size)
        return finder.validator.validateTargets(preview.state, selection.targets, aligned, controller,
            preview.card.colors, preview.subtypes, preview.copyId, preview.xValue,
            TargetingSourceType.SPELL, retainedTargetIndices = selection.retained)
            ?: unseatedKeptTarget(preview, finder, aligned, originals(state, sourceId, ordinal), selection)
    }

    /**
     * New targets must not make an unchanged target illegal (CR 115.7d): keeping the land of
     * "target land that player controls" while changing the opponent is only allowed when the land
     * was already illegal, or still is legal under the new opponent.
     */
    private fun unseatedKeptTarget(preview: Preview, finder: TargetFinder, aligned: List<TargetRequirement>,
                                   originals: List<ChosenTarget>, selection: Selection): String? {
        val unseated = selection.retained.sorted().firstOrNull { slot ->
            unseats(preview, finder, aligned, originals, slot, selection.targets)
        } ?: return null
        return "The new targets would make an unchanged target illegal: ${slotRequirement(aligned, unseated)?.description}"
    }

    /** Whether [bindings] make the original target in [slot] illegal when it was legal among the originals. */
    private fun unseats(preview: Preview, finder: TargetFinder, aligned: List<TargetRequirement>,
                        originals: List<ChosenTarget>, slot: Int, bindings: List<ChosenTarget>): Boolean {
        val requirement = slotRequirement(aligned, slot) ?: return false
        val original = originals.getOrNull(slot) ?: return false
        fun legal(among: List<ChosenTarget>) = finder.validator.validateSingleTarget(
            state = preview.state, target = original, requirement = requirement,
            casterId = preview.controllerId, sourceColors = preview.card.colors, sourceSubtypes = preview.subtypes,
            sourceId = preview.copyId, xValue = preview.xValue, allTargets = among,
            targetingSourceType = TargetingSourceType.SPELL,
            namedTargets = EffectContext.buildNamedTargets(aligned, among),
        ) == null
        return legal(originals) && !legal(bindings)
    }

    private fun slotRequirement(aligned: List<TargetRequirement>, slot: Int): TargetRequirement? =
        aligned.flatMap { req -> List(req.count) { req } }.getOrNull(slot)

    // The last requirement owns the remaining targets, including an unbounded/X group.
    // Earlier groups use the recorded counts, as do stack resolution and named-target binding.
    fun alignedRequirements(requirements: List<TargetRequirement>, size: Int): List<TargetRequirement> {
        var left = size
        return requirements.mapIndexed { i, req ->
            val count = if (i == requirements.lastIndex) left else minOf(req.count, left)
            left -= count
            req.withCount(count)
        }
    }

    fun ChosenTarget.entityId(): EntityId = when (this) {
        is ChosenTarget.Player -> playerId
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Spell -> spellEntityId
        is ChosenTarget.Card -> cardId
    }

    fun legalTargets(
        state: GameState,
        finder: TargetFinder,
        sourceId: EntityId,
        controllerId: EntityId,
        requirements: List<TargetRequirement>,
        exceptions: CopyExceptions,
    ): Map<Int, List<EntityId>> {
        val preview = preview(state, sourceId, controllerId, exceptions) ?: return emptyMap()
        return requirements.mapIndexed { index, requirement -> index to preview.legal(finder, requirement) }.toMap()
    }
}
