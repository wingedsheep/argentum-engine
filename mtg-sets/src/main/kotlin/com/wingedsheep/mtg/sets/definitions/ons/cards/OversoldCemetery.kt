package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Oversold Cemetery
 * {1}{B}
 * Enchantment
 * At the beginning of your upkeep, if you have four or more creature cards
 * in your graveyard, you may return target creature card from your graveyard to your hand.
 */
val OversoldCemetery = card("Oversold Cemetery") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, if you have four or more creature cards in your graveyard, you may return target creature card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        triggerCondition = Conditions.CreatureCardsInGraveyardAtLeast(4)
        val t = target("target", TargetObject(
            filter = TargetFilter.CreatureInYourGraveyard
        ))
        effect = ConditionalEffect(
            condition = Conditions.CreatureCardsInGraveyardAtLeast(4),
            effect = Effects.Move(
                target = t,
                destination = Zone.HAND
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Thomas M. Baxa"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bbfd715-0772-4516-8cd8-89495dbccf4a.jpg?1562909019"
    }
}
