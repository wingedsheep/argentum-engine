package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Akron Legionnaire
 * {6}{W}{W}
 * Creature — Giant Soldier
 * 8/4
 * Except for creatures named Akron Legionnaire and artifact creatures, creatures you control can't attack.
 */
val AkronLegionnaire = card("Akron Legionnaire") {
    manaCost = "{6}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant Soldier"
    power = 8
    toughness = 4
    oracleText = "Except for creatures named Akron Legionnaire and artifact creatures, creatures you control can't attack."

    staticAbility {
        ability = CantAttack(
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl()
                    .nonartifact()
                    .notNamed("Akron Legionnaire")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d074af2-8dbd-42d3-87eb-30f6e7d171ff.jpg?1783948089"
        ruling("2004-10-04", "If you have two or more Legionnaires, they can all attack.")
    }
}
