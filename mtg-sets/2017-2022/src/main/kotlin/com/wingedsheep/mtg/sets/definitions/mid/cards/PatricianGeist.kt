package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Patrician Geist
 * {2}{U}
 * Creature — Spirit Knight
 * 2/2
 * Flying
 * Other Spirits you control get +1/+1.
 * Spells you cast from your graveyard cost {1} less to cast.
 */
val PatricianGeist = card("Patrician Geist") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Spirit Knight"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Other Spirits you control get +1/+1.\n" +
        "Spells you cast from your graveyard cost {1} less to cast."

    keywords(Keyword.FLYING)

    // Other Spirits you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.SPIRIT).youControl(),
                excludeSelf = true
            )
        )
    }

    // Spells you cast from your graveyard cost {1} less to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCastFromZones(setOf(Zone.GRAVEYARD)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Marta Nael"
        flavorText = "He rides in agitated circles, snapping orders at servants who've long since found their peace."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de2b0c63-c929-42ec-aada-dddb2a26550c.jpg?1783925634"
    }
}
