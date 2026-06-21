package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Add Job select (Final Fantasy) — keyword + enters-the-battlefield triggered ability.
 *
 * "Job select (When this Equipment enters, create a 1/1 colorless Hero creature token,
 * then attach this to it.)"
 *
 * The keyword is display-only (no separate Job-select handler exists); the behavior is
 * composed here from two existing primitives chained through the token pipeline:
 *
 *  1. [com.wingedsheep.sdk.scripting.effects.CreateTokenEffect] makes a 1/1 colorless Hero
 *     token and publishes its entity id to the [CREATED_TOKENS] pipeline slot (the same slot
 *     Outlaw Stitcher / Mardu Monument read to address tokens they just made).
 *  2. [com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect] attaches the source
 *     Equipment (`context.sourceId`) to that freshly-created token, read back out of the
 *     pipeline via `EffectTarget.PipelineTarget(CREATED_TOKENS, 0)`.
 *
 * This is the shared shell for the whole Job-select Equipment cycle; the per-card equip cost
 * and equipped-creature bonus are authored on the card alongside this call.
 */
fun CardBuilder.jobSelect() {
    keywordSet.add(Keyword.JOB_SELECT)
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.EntersBattlefield.event,
            binding = Triggers.EntersBattlefield.binding,
            effect = Effects.Composite(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = emptySet(),
                    creatureTypes = setOf("Hero")
                ),
                Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
            ),
            descriptionOverride = "Job select (When this Equipment enters, create a 1/1 " +
                "colorless Hero creature token, then attach this to it.)"
        )
    )
}
