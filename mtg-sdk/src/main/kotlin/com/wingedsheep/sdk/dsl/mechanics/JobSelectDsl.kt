package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scryfall artwork for the 1/1 colorless Hero token every Job-select Equipment creates (FIN `tfin`
 * #2). Defined once here so the whole cycle shares one image rather than duplicating it per card.
 */
private const val JOB_SELECT_HERO_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/d/0/d0657ce1-bf75-4007-ac1b-0623eb263357.jpg?1748704030"

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
fun CardBuilder.jobSelect() = equipmentMakesItsOwnBearer(
    keyword = Keyword.JOB_SELECT,
    token = Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = emptySet(),
        creatureTypes = setOf("Hero"),
        imageUri = JOB_SELECT_HERO_TOKEN_IMAGE
    ),
    reminderText = "Job select (When this Equipment enters, create a 1/1 " +
        "colorless Hero creature token, then attach this to it.)"
)

/**
 * The shell shared by [jobSelect] and [forMirrodin]: add [keyword] (display-only) plus an
 * enters-the-battlefield trigger that runs [token] — which publishes the new token's id to the
 * [CREATED_TOKENS] pipeline slot — then attaches this Equipment to that token.
 */
internal fun CardBuilder.equipmentMakesItsOwnBearer(
    keyword: Keyword,
    token: CreateTokenEffect,
    reminderText: String,
) {
    keywordSet.add(keyword)
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.self.enters(),
            effect = token then
                Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)),
            descriptionOverride = reminderText
        )
    )
}
