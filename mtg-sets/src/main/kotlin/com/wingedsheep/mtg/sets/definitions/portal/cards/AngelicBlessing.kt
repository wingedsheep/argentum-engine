package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angelic Blessing
 * {2}{W}
 * Sorcery
 * Target creature gets +3/+3 and gains flying until end of turn.
 */
val AngelicBlessing = card("Angelic Blessing") {
    manaCost = "{2}{W}"
    typeLine = "Sorcery"

    spell {
        target = Targets.Creature
        effect = Effects.Composite(
            Effects.ModifyStats(3, 3, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "DiTerlizzi"
        flavorText = "A peasant can do more by faith than a king by force."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31dda640-2a00-437e-855f-173c487e7395.jpg"
    }
}
