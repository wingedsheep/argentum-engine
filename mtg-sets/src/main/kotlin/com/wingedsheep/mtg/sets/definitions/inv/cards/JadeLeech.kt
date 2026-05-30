package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Jade Leech
 * {2}{G}{G}
 * Creature — Leech
 * 5/5
 * Green spells you cast cost {G} more to cast.
 */
val JadeLeech = card("Jade Leech") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Leech"
    power = 5
    toughness = 5
    oracleText = "Green spells you cast cost {G} more to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.GREEN)),
            modification = CostModification.IncreaseColored("{G}"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "John Howe"
        flavorText = "\"It took magic to extract one of the stones and five people to carry it.\"\n—Tolarian research notes"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3392171d-ed25-46a1-91cc-a4f24537617d.jpg?1562905388"
    }
}
