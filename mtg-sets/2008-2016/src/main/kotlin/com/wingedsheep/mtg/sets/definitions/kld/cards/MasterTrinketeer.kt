package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Master Trinketeer
 * {2}{W}
 * Creature — Dwarf Artificer
 * 3/2
 * Servos and Thopters you control get +1/+1.
 * {3}{W}: Create a 1/1 colorless Servo artifact creature token.
 */
val MasterTrinketeer = card("Master Trinketeer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Artificer"
    oracleText = "Servos and Thopters you control get +1/+1.\n" +
        "{3}{W}: Create a 1/1 colorless Servo artifact creature token."
    power = 3
    toughness = 2

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature
                    .withAnyOfSubtypes(listOf(Subtype("Servo"), Subtype.THOPTER))
                    .youControl()
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Matt Stewart"
        flavorText = "\"Let us never forget the joy that lies at the heart of invention.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdb6beb3-1669-4e7b-9c76-65424673eb36.jpg?1783937230"
        ruling("2016-09-20", "A creature that is both a Servo and a Thopter gets +1/+1, not +2/+2.")
    }
}
