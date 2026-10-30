package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Flutterfox
 * {1}{W}
 * Creature — Fox
 * 2/2
 *
 * As long as you control an artifact or enchantment, this creature has flying.
 */
val Flutterfox = card("Flutterfox") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox"
    oracleText = "As long as you control an artifact or enchantment, this creature has flying."
    power = 2
    toughness = 2

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.ArtifactOrEnchantment)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Lucas Graciano"
        flavorText = "\"The white fox grinned. 'How about a bet, dragon? If I win, the skies are mine.' After he stopped laughing, the dragon agreed.\"\n—*Tales of the Fae*"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/769cddf0-2078-456b-a622-057571464fe7.jpg?1783932678"
    }
}
