package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade

/**
 * The trigger half of a **granted** offspring (CR 702.175a) as pure data — "When this permanent
 * enters, if its offspring cost was paid, create a token that's a copy of it, except it's 1/1."
 *
 * A printed offspring card authors this trigger itself, gated on its own kicker-rail payment. A
 * spell that only *gained* offspring as it was cast ([GrantOffspringToOwnSpells], Zinnia, Valley's
 * Voice) has no card to print it on, so the engine grants [tokenCopyAbility] to the permanent such a
 * spell becomes whenever its cast stamped [ChoiceSlot.OFFSPRING] — the same synthesize-from-the-
 * rules shape as [Sieges.defeatAbility].
 */
object Offspring {

    /**
     * The synthesized offspring trigger. The intervening "if" re-reads the durable cast choice, so
     * it is checked both when the permanent enters and as the trigger resolves (CR 603.4).
     */
    val tokenCopyAbility: TriggeredAbility = Triggers.self.enters().let { spec ->
        TriggeredAbility(
            id = AbilityId("granted_offspring"),
            trigger = spec.event,
            binding = spec.binding,
            interveningIf = CastChoiceMade(ChoiceSlot.OFFSPRING),
            effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1),
            descriptionOverride = "Offspring — when this permanent enters, if its offspring cost was paid, " +
                "create a token that's a copy of it, except it's 1/1.",
        )
    }
}
