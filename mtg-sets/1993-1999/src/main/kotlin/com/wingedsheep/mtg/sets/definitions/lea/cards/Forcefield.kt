package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

val Forcefield = card("Forcefield") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{1}: The next time an unblocked creature of your choice would deal combat damage to you this turn, prevent all but 1 of that damage."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.PreventNextDamageLeavingAmount(
            amountToLeave = DynamicAmounts.fixed(1),
            eligibleSource = GameObjectFilter.Creature.unblocked(),
            combatOnly = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f2004c1-8efe-407f-bf48-27b807422eea.jpg?1783948667"
        ruling("2004-10-04", "This can’t be used to prevent damage caused by a blocked creature with Trample ability.")
    }
}
