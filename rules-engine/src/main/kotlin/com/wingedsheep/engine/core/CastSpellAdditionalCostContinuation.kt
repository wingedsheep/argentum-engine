package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * The selection-requiring additional-cost atoms a server-initiated free cast must still let the
 * caster pay. "Cast without paying its mana cost" waives only the *mana* cost (CR 601.2f /
 * 118.9); printed additional costs are paid as normal. Each kind maps 1:1 to a field of
 * [com.wingedsheep.sdk.scripting.AdditionalCostPayment].
 */
@Serializable
enum class AdditionalCostSelectionKind { SACRIFICE, DISCARD, EXILE, TAP, RETURN_TO_HAND }

/**
 * Pause/resume frame for choosing how to pay one selection-requiring additional cost during a
 * free cast (Roving Actuator / Shiko / Cascade copy-and-cast pipelines call
 * [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler.execute] directly with no
 * [com.wingedsheep.sdk.scripting.AdditionalCostPayment], so the selection isn't supplied by the
 * client the way a normal cast supplies it).
 *
 * On resume the chosen entities are merged into [baseCastAction]'s payment for [costKind], and
 * `execute()` is re-entered. The re-entry re-derives the unpaid costs and either pauses for the
 * next one or proceeds to pay — exactly mirroring the modal cast-time re-entry pattern
 * ([CastModalTargetSelectionContinuation]). The pause sits before any cost is paid, so it carries
 * no partial side effects.
 */
@Serializable
data class CastSpellAdditionalCostContinuation(
    val cardId: EntityId,
    val casterId: EntityId,
    val baseCastAction: CastSpell,
    val costKind: AdditionalCostSelectionKind,
) : AnswerContinuation

/** Announcement of a named additional-cost branch, before any payment is made. */
@Serializable
data class CastCostChoiceContinuation(
    val baseCastAction: CastSpell,
    val slot: com.wingedsheep.sdk.scripting.ChoiceSlot,
    val offeredIndices: List<Int>,
) : AnswerContinuation
