package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mischief and Mayhem
 * {4}{G}
 * Sorcery
 *
 * Up to two target creatures each get +4/+4 until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val MischiefAndMayhem = card("Mischief and Mayhem") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Up to two target creatures each get +4/+4 until end of turn."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(4, 4, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Zack Stella"
        flavorText = "\"The false god Xenagos treats the whole world as his rollick site. If his whims are left unchecked, Theros will be reduced to rubble and ashes.\"\n—Elspeth"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9daa44b4-5c30-41d8-b4d6-6219c5729a5f.jpg?1783939533"
    }
}
