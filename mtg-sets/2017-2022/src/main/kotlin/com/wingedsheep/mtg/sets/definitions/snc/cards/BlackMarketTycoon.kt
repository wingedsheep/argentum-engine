package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Black Market Tycoon
 * {R}{G}
 * Creature — Cat Rogue
 * 2/2
 *
 * At the beginning of your upkeep, this creature deals 2 damage to you for each Treasure you control.
 * {T}: Create a Treasure token.
 *
 * The upkeep trigger has no intervening "if" — it fires with zero Treasures and counts them on
 * resolution (the amount is a resolution-time battlefield count, doubled via `* 2`).
 */
val BlackMarketTycoon = card("Black Market Tycoon") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Cat Rogue"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your upkeep, this creature deals 2 damage to you for each Treasure you control.\n" +
        "{T}: Create a Treasure token."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact.withSubtype("Treasure")).count() * 2,
            EffectTarget.Controller,
            damageSource = EffectTarget.Self
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "167"
        artist = "Brock Grossman"
        flavorText = "\"I have exactly what you need. The question is, how badly do you need it?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28d92b13-8b23-4c78-b690-c5d9ca835809.jpg?1783923094"
        ruling("2022-04-29", "Black Market Tycoon's ability triggers at the beginning of your upkeep whether or not you actually have any Treasures, and it counts the number of Treasures you have as it resolves.")
    }
}
