package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Hide on the Ceiling
 * {X}{U}
 * Instant
 * Exile X target artifacts and/or creatures. Return the exiled cards to the
 * battlefield under their owners' control at the beginning of the next end step.
 */
val HideOnTheCeiling = card("Hide on the Ceiling") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile X target artifacts and/or creatures. Return the exiled cards to the battlefield under their owners' control at the beginning of the next end step."

    spell {
        // "X target artifacts and/or creatures" — exactly X.
        targets(TargetFilter.CreatureOrArtifact, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(
            Patterns.Exile.exileUntilEndStep(EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Fariba Khamseh"
        flavorText = "\"Peter? Is that you? I could have sworn I heard that boy moving around up here.\"\n—Aunt May"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7977e448-01fa-4fa5-a275-0d6a1357b35c.jpg?1757376939"
    }
}
