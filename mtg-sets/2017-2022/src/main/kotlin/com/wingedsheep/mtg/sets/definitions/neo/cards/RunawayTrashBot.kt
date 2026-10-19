package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Runaway Trash-Bot
 * {3}
 * Artifact Creature — Construct
 * 0/4
 * Trample
 * This creature gets +1/+0 for each artifact and/or enchantment card in your graveyard.
 *
 * The Knight of the Reliquary shape: a [GrantDynamicStats] on [GroupFilter.source] counting
 * cards in your graveyard. [GameObjectFilter.ArtifactOrEnchantment] is a single `Or` predicate,
 * so an artifact enchantment card is counted once, per the ruling.
 */
val RunawayTrashBot = card("Runaway Trash-Bot") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 4
    oracleText = "Trample\n" +
        "This creature gets +1/+0 for each artifact and/or enchantment card in your graveyard."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.zone(
                Player.You,
                Zone.GRAVEYARD,
                GameObjectFilter.ArtifactOrEnchantment
            ).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Nicholas Gregory"
        flavorText = "One day it might roll over your foot, the next day your entire house."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3eebf6a6-f1fd-40a1-ac27-367deafa6f2a.jpg?1783923822"
        ruling("2022-02-18", "A card in your graveyard that is both an artifact and an enchantment is only counted once.")
    }
}
