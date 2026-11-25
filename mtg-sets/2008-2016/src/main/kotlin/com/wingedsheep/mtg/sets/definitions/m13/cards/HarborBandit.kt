package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harbor Bandit
 * {2}{B}
 * Creature — Human Rogue
 * 2/2
 * This creature gets +1/+1 as long as you control an Island.
 * {1}{U}: This creature can't be blocked this turn.
 */
val HarborBandit = card("Harbor Bandit") {
    manaCost = "{2}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    oracleText = "This creature gets +1/+1 as long as you control an Island.\n" +
        "{1}{U}: This creature can't be blocked this turn."
    power = 2
    toughness = 2

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island")),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Jesper Ejsing"
        flavorText = "He always drops a little trinket into the bay as thanks to the merfolk who taught him all his best tricks."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8422e109-de8d-46ea-a7f8-d5ccb6340497.jpg"
    }
}
