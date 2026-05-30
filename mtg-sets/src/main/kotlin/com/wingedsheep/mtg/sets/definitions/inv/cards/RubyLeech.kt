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
 * Ruby Leech
 * {1}{R}
 * Creature — Leech
 * 2/2
 * First strike
 * Red spells you cast cost {R} more to cast.
 */
val RubyLeech = card("Ruby Leech") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Leech"
    power = 2
    toughness = 2
    oracleText = "First strike\nRed spells you cast cost {R} more to cast."

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.RED)),
            modification = CostModification.IncreaseColored("{R}"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Jacques Bredy"
        flavorText = "\"Its gems didn't stop pulsating until they were completely removed.\"\n—Tolarian research notes"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be621b12-4f4e-43a6-b65e-da4223e742b5.jpg?1562933305"
    }
}
