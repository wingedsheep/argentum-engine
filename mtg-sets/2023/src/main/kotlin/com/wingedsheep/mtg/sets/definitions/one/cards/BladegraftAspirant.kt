package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Bladegraft Aspirant
 * {2}{R}
 * Creature — Phyrexian Warrior
 * 2/3
 *
 * Menace
 * Equipment spells you cast cost {1} less to cast.
 * Activated abilities of Equipment you control that target this creature cost {1} less to activate.
 */
val BladegraftAspirant = card("Bladegraft Aspirant") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Phyrexian Warrior"
    power = 2
    toughness = 3
    oracleText = "Menace\n" +
        "Equipment spells you cast cost {1} less to cast.\n" +
        "Activated abilities of Equipment you control that target this creature cost {1} less to activate."

    keywords(Keyword.MENACE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
            amount = DynamicAmounts.fixed(1),
            onlyIfTargetIsSource = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Valera Lutfullina"
        flavorText = "\"To dance to the music of Juex, leap like the flickering flame and hit like the hammer.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d6cf933-73fe-4e79-8d0b-29dc0f18b25d.jpg?1783918035"
    }
}
