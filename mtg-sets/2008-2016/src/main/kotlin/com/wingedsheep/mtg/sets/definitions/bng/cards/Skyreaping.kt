package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skyreaping
 * {1}{G}
 * Sorcery
 *
 * Skyreaping deals damage to each creature with flying equal to your devotion to green.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing (later reprinted in the
 * unscaffolded masters set PIO). Devotion is counted as the spell resolves.
 */
val Skyreaping = card("Skyreaping") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Skyreaping deals damage to each creature with flying equal to your devotion to green. " +
        "(Each {G} in the mana costs of permanents you control counts toward your devotion to green.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures.withKeyword(Keyword.FLYING),
            Effects.DealDamage(DynamicAmounts.devotionTo(Color.GREEN), EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40eb76b3-b527-4ed8-8ce3-d3de48562b6e.jpg?1783939529"
    }
}
