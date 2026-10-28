package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MultiplyManaOnSourceTap

/**
 * Nyxbloom Ancient
 * {4}{G}{G}{G}
 * Enchantment Creature — Elemental
 * 5/5
 * Trample
 * If you tap a permanent for mana, it produces three times as much of that mana instead.
 *
 * Virtue of Strength's [MultiplyManaOnSourceTap] widened to every permanent. The filter is read
 * from Nyxbloom's controller, so `.youControl()` is the printed "If you tap"; an opponent's
 * Nyxbloom never multiplies your mana. Every shape of mana a tapped permanent makes is scaled —
 * a conditional ability's chosen branch, and "one mana of each color among …" (Bloom Tender).
 */
val NyxbloomAncient = card("Nyxbloom Ancient") {
    manaCost = "{4}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Elemental"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "If you tap a permanent for mana, it produces three times as much of that mana instead."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = MultiplyManaOnSourceTap(
            sourceFilter = GameObjectFilter.Permanent.youControl(),
            multiplier = 3
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "190"
        artist = "Filip Burburan"
        flavorText = "Ancient yet ever-young, it is wise and subtle—and cruel and reckless as the spring itself."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a391da36-0b40-46ea-b771-50d2b920207e.jpg?1783931531"
    }
}
