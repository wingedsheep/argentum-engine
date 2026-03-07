package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trail of Mystery
 * {1}{G}
 * Enchantment
 * Whenever a face-down creature you control enters, you may search your library for a basic
 * land card, reveal it, put it into your hand, then shuffle.
 * Whenever a permanent you control is turned face up, if it's a creature, it gets +2/+2
 * until end of turn.
 */
val TrailOfMystery = card("Trail of Mystery") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment"
    oracleText = "Whenever a face-down creature you control enters, you may search your library for a basic land card, reveal it, put it into your hand, then shuffle.\nWhenever a permanent you control is turned face up, if it's a creature, it gets +2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.FaceDownCreatureEnters.youControl()
        effect = MayEffect(
            EffectPatterns.searchLibrary(
                filter = Filters.BasicLand,
                count = 1,
                reveal = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.CreatureTurnedFaceUp()
        effect = Effects.ModifyStats(2, 2, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6149685f-cb05-4c1f-95a4-3810505d1a95.jpg?1562787478"
    }
}
