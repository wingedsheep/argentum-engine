package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Epic Downfall
 * {1}{B}
 * Sorcery
 *
 * Exile target creature with mana value 3 or greater.
 *
 * Canonical earliest real printing is Throne of Eldraine (ELD, 2019); reprinted in
 * Avatar: The Last Airbender (TLA) — see EpicDownfallReprint in the TLA package.
 */
val EpicDownfall = card("Epic Downfall") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile target creature with mana value 3 or greater."

    spell {
        val creature = target(TargetFilter.Creature.manaValueAtLeast(3))
        effect = Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Eric Deschamps"
        flavorText = "The dragon had a lot of things going through his mind that day. He didn't expect a sword to be one of them."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63da83fe-fa59-40cb-a42e-e1b14b650bc8.jpg?1783932642"
    }
}
