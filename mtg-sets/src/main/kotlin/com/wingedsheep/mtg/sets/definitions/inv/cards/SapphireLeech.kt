package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Sapphire Leech
 * {1}{U}
 * Creature — Leech
 * 2/2
 * Flying
 * Blue spells you cast cost {U} more to cast.
 */
val SapphireLeech = card("Sapphire Leech") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Leech"
    power = 2
    toughness = 2
    oracleText = "Flying\nBlue spells you cast cost {U} more to cast."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.BLUE)),
            modification = CostModification.IncreaseColored("{U}"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Ron Spencer"
        flavorText = "\"The subject's wings are clearly vestigial. We suspect the gems somehow keep it aloft.\"\n—Tolarian research notes"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6763ffd-9d89-4f26-871a-be24fbdef38d.jpg?1562941279"
    }
}
