package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heavy Mattock
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * As long as equipped creature is a Human, it gets an additional +1/+1.
 * Equip {2}
 */
val HeavyMattock = card("Heavy Mattock") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText =
        "Equipped creature gets +1/+1.\n" +
            "As long as equipped creature is a Human, it gets an additional +1/+1.\n" +
            "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }
    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.withSubtype(Subtype.HUMAN),
        )
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }
    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Winona Nelson"
        imageUri =
            "https://cards.scryfall.io/normal/front/8/b/8b09df01-0b3e-463e-bf00-0a9f1822261a.jpg?1783940792"
    }
}
