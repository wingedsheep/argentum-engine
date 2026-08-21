package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.TurnUpProcedure
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.FaceDownMode

/**
 * Derives the [MorphDataComponent] that lets a face-down permanent be turned face up, given the
 * card it represents and the [FaceDownMode] under which it entered the battlefield.
 *
 * This is the one place that knows the turn-up rule of each face-down mechanic. The turn-up itself
 * (special action, payment, flip) is mechanic-agnostic — it reads only [MorphDataComponent] — so a
 * manifested, cloaked or disguised creature reuses the entire morph turn-up machinery for free.
 *
 * Returns `null` when the permanent has no way to be turned face up (a non-morph card entered as
 * morph, a manifested non-creature card per CR 701.40b, or [FaceDownMode.HIDDEN]).
 */
object FaceDownTurnUp {

    fun dataFor(
        cardDef: CardDefinition?,
        cardDefinitionId: String,
        mode: FaceDownMode
    ): MorphDataComponent? {
        val procedures = proceduresFor(cardDef, mode)
        return if (procedures.isEmpty()) null else MorphDataComponent(procedures, cardDefinitionId)
    }

    /**
     * Every turn-up procedure available to a permanent that went face down under [mode].
     *
     * Manifest and cloak contribute their own "pay the card's mana cost" procedure *and* keep any
     * morph/disguise procedure the card underneath prints — CR 701.40c/d and 701.58c/d explicitly
     * let the controller choose either. Morph and disguise are single-procedure mechanics.
     */
    private fun proceduresFor(cardDef: CardDefinition?, mode: FaceDownMode): List<TurnUpProcedure> =
        when (mode) {
            // Morph / Megamorph (CR 702.37e): turn face up by paying the card's morph cost.
            FaceDownMode.MORPH -> listOfNotNull(morphProcedure(cardDef))

            // Disguise (CR 702.168d): turn face up by paying the card's disguise cost. The ward {2}
            // the mechanic also grants is a face-down *characteristic*, carried by the mode itself
            // (FaceDownMode.faceDownWard), not a turn-up concern.
            FaceDownMode.DISGUISE -> listOfNotNull(disguiseProcedure(cardDef))

            // Manifest (CR 701.40b) / Cloak (CR 701.58b): turn face up by paying the card's mana
            // cost, but only if the card representing the permanent is a creature card. A
            // manifested or cloaked non-creature card can never be turned face up that way — but
            // if it prints morph or disguise, that procedure is still available (701.40c/d,
            // 701.58c/d), and for a creature card both are offered side by side.
            FaceDownMode.MANIFEST, FaceDownMode.CLOAK -> listOfNotNull(
                if (cardDef != null && cardDef.typeLine.isCreature) {
                    TurnUpProcedure(PayCost.Atom(CostAtom.Mana(cardDef.manaCost)), mode)
                } else {
                    null
                },
                morphProcedure(cardDef),
                disguiseProcedure(cardDef)
            )

            // Exiled / hidden face down — no in-place turn-up procedure.
            FaceDownMode.HIDDEN -> emptyList()
        }

    private fun morphProcedure(cardDef: CardDefinition?): TurnUpProcedure? =
        cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
            ?.let { TurnUpProcedure(it.morphCost, FaceDownMode.MORPH, it.faceUpEffect) }

    private fun disguiseProcedure(cardDef: CardDefinition?): TurnUpProcedure? =
        cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Disguise>()?.firstOrNull()
            ?.let {
                TurnUpProcedure(it.disguiseCost, FaceDownMode.DISGUISE, it.faceUpEffect, it.costReduction)
            }
}
