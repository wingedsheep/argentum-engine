package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

val RentIsDue = card("Rent Is Due") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, tap two untapped creatures and/or Treasures you control. If you do, draw a card. Otherwise, sacrifice Rent Is Due."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        val tapCost = Effects.Pipeline {
            val rentTargets = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = (GameObjectFilter.Creature or GameObjectFilter.Artifact.withSubtype("Treasure")).untapped()
                ),
                name = "rentTargets"
            )
            val toTap = chooseExactly(
                2, from = rentTargets,
                prompt = "Tap two untapped creatures and/or Treasures you control",
                useTargetingUI = true,
                name = "toTap"
            )
            run(TapUntapCollectionEffect("toTap", tap = true))
        }
        effect = OptionalCostEffect(
            cost = tapCost,
            ifPaid = Effects.DrawCards(1),
            ifNotPaid = SacrificeSelfEffect,
            descriptionOverride = "Tap two untapped creatures and/or Treasures you control. If you do, draw a card. Otherwise, sacrifice Rent Is Due."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Gal Or"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3f8d221-081f-49f5-a501-07e5eb21a840.jpg?1757376808"
    }
}
