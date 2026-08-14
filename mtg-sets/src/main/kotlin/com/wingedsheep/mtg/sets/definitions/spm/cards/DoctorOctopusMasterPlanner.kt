package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Doctor Octopus, Master Planner
 * {5}{U}{B}
 * Legendary Creature — Human Scientist Villain
 * 4/8
 * Other Villains you control get +2/+2.
 * Your maximum hand size is eight.
 * At the beginning of your end step, if you have fewer than eight cards in hand,
 * draw cards equal to the difference.
 */
val DoctorOctopusMasterPlanner = card("Doctor Octopus, Master Planner") {
    manaCost = "{5}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Scientist Villain"
    power = 4
    toughness = 8
    oracleText = "Other Villains you control get +2/+2.\n" +
        "Your maximum hand size is eight.\n" +
        "At the beginning of your end step, if you have fewer than eight cards in hand, " +
        "draw cards equal to the difference."

    // Other Villains you control get +2/+2.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Villain").youControl(),
                excludeSelf = true,
            ),
        )
    }

    // Your maximum hand size is eight.
    staticAbility {
        ability = SetMaximumHandSize(player = Player.You, amount = DynamicAmount.Fixed(8))
    }

    // At the beginning of your end step, if you have fewer than eight cards in hand,
    // draw cards equal to the difference.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.CardsInHandAtMost(7)
        effect = Effects.DrawCards(
            DynamicAmount.Subtract(
                DynamicAmount.Fixed(8),
                DynamicAmount.Count(Player.You, Zone.HAND),
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "128"
        artist = "Xabi Gaztelua"
        flavorText = "\"Soon all will cower before the superior intellect of Doctor Octopus!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76e1d361-18a1-4dec-a203-f83bf0014e02.jpg?1783905317"
    }
}
