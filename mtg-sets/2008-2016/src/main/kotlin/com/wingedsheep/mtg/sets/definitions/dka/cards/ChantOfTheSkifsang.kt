package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Chant of the Skifsang
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -13/-0.
 */
val ChantOfTheSkifsang = card("Chant of the Skifsang") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -13/-0."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-13, 0)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Nils Hamm"
        flavorText = "The skifsang, seafarers of Nephalia, craft spells like other sailors craft nets—making them strong enough to snare even the deadliest catch."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e604b2e-f257-465d-9342-6eb55b2334c5.jpg"
    }
}
