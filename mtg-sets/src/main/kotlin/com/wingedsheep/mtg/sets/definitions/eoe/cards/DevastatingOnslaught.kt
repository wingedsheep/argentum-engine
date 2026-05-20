package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Devastating Onslaught
 * {X}{X}{R}
 * Sorcery
 *
 * Create X tokens that are copies of target artifact or creature you control.
 * Those tokens gain haste until end of turn. Sacrifice them at the beginning
 * of the next end step.
 *
 * Implementation note: each token is created with permanent haste plus a granted
 * "At the beginning of the end step, sacrifice this token" triggered ability.
 * Since the very next end step sacrifices the token, the haste-until-end-of-turn
 * window is indistinguishable from permanent haste in practice.
 */
val DevastatingOnslaught = card("Devastating Onslaught") {
    manaCost = "{X}{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create X tokens that are copies of target artifact or creature you control. " +
        "Those tokens gain haste until end of turn. Sacrifice them at the beginning of the next end step."

    val sacrificeAtEndStep = TriggeredAbility.create(
        trigger = Triggers.EachEndStep.event,
        binding = Triggers.EachEndStep.binding,
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    )

    spell {
        val t = target(
            "target artifact or creature you control",
            TargetPermanent(filter = TargetFilter.CreatureOrArtifact.youControl())
        )
        effect = CreateTokenCopyOfTargetEffect(
            target = t,
            count = DynamicAmount.XValue,
            addedKeywords = setOf(Keyword.HASTE),
            triggeredAbilities = listOf(sacrificeAtEndStep)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "132"
        artist = "Chris Seaman"
        flavorText = "\"Adapt to this, you bug-eyed planet-squatters!\"\n—Admiral Scargruf, Kavaron Memorial Navy"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc779971-24e2-46a8-86be-16f0b244a3d2.jpg?1752947086"

        ruling("2025-07-25", "Any enters abilities of the copied permanent will trigger when the tokens enter. Any \"as [this permanent] enters\" or \"[this permanent] enters with\" abilities of the copied permanent will also work.")
        ruling("2025-07-25", "The tokens copy exactly what was printed on the original permanent and nothing else (unless that permanent is itself copying something else). They don't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on. If the permanent is a Vehicle, the copies are not crewed. If it is an Equipment, the copies are not attached to any creature.")
        ruling("2025-07-25", "If the copied permanent is a token, the tokens that are created copy the original characteristics of that token as stated by the effect that created the token.")
        ruling("2025-07-25", "If the copied permanent is copying something else, then the tokens enter as whatever that permanent copied.")
    }
}
