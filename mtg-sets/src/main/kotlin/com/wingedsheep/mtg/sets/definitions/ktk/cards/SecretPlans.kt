package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Secret Plans
 * {G}{U}
 * Enchantment
 * Face-down creatures you control get +0/+1.
 * Whenever a permanent you control is turned face up, draw a card.
 */
val SecretPlans = card("Secret Plans") {
    manaCost = "{G}{U}"
    typeLine = "Enchantment"
    oracleText = "Face-down creatures you control get +0/+1.\nWhenever a permanent you control is turned face up, draw a card."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 0,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().faceDown())
        )
    }

    triggeredAbility {
        trigger = Triggers.CreatureTurnedFaceUp()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Daarken"
        flavorText = "Rakshasa trade in secrets, amassing wealth from their careful revelation."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01589046-a969-400f-b4ac-90cbbb814504.jpg?1562781791"
    }
}
