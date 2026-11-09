package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * True-Faith Censer
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +1/+1 and has vigilance.
 * As long as equipped creature is a Human, it gets an additional +1/+0.
 * Equip {2}
 */
val TrueFaithCenser = card("True-Faith Censer") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText =
        "Equipped creature gets +1/+1 and has vigilance.\n" +
            "As long as equipped creature is a Human, it gets an additional +1/+0.\n" +
            "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }
    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.withSubtype(Subtype.HUMAN),
        )
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }
    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "267"
        artist = "Christine Choi"
        imageUri =
            "https://cards.scryfall.io/normal/front/8/1/815eb91c-06fa-4362-803e-e9bf7d374856.jpg?1783937701"
    }
}
