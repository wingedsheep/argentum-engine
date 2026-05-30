package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Alabaster Leech
 * {W}
 * Creature — Leech
 * 1/3
 * White spells you cast cost {W} more to cast.
 */
val AlabasterLeech = card("Alabaster Leech") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Leech"
    power = 1
    toughness = 3
    oracleText = "White spells you cast cost {W} more to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.WHITE)),
            modification = CostModification.IncreaseColored("{W}"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"Its stones seem to serve a healing function, but removing them intact is an exhausting process.\"\n—Tolarian research notes"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c86b45d9-aba6-4c09-8605-037754ba7fd4.jpg?1562935200"
    }
}
