package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.effects.SourceReferenceText
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * The prompt text for an "any player may [cost]" offer ([com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect]),
 * built from the effect's own data rather than a fixed sentence: what the payment is, then what
 * paying does and what happens if nobody pays.
 *
 * Shared by the executor (first player asked) and the continuation resumer (every later player),
 * so both prompts read the same.
 *
 * - Innocent Traveler: "You may sacrifice a creature. If no one does, transform Innocent Traveler."
 * - Prowling Pangolin: "You may sacrifice two creatures. If you do, sacrifice Prowling Pangolin."
 * - Reservoir Kraken: "You may tap an untapped creature you control. If you do, tap Reservoir Kraken and …"
 */
object AnyPlayerMayPayPrompt {

    fun selection(
        cost: CostAtom,
        sourceName: String,
        consequence: Effect?,
        consequenceIfNonePaid: Effect?
    ): String = buildString {
        append("You may ${cost.description}.")
        if (consequence != null) append(" If you do, ${consequenceText(consequence, sourceName)}.")
        if (consequenceIfNonePaid != null) append(" If no one does, ${consequenceText(consequenceIfNonePaid, sourceName)}.")
    }

    /**
     * The yes/no a free choice (null cost) puts to [playerId] — the effect's own [prompt], answered
     * "Yes" / "No". Built here so the first player and every later one see the same question.
     */
    fun freeChoice(
        prompt: String,
        playerId: EntityId,
        sourceId: EntityId,
        sourceName: String
    ): (String) -> YesNoDecision = { decisionId ->
        YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Yes",
            noText = "No"
        )
    }

    /** [effect]'s text as a clause, the source permanent named — see [SourceReferenceText.clause]. */
    private fun consequenceText(effect: Effect, sourceName: String): String =
        SourceReferenceText.clause(effect, sourceName)
}
