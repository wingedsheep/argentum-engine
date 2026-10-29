package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Waker of Waves (Core Set 2021 #84)
 * {5}{U}{U} · Creature — Whale · 7/7
 *
 * Creatures your opponents control get -1/-0.
 * {1}{U}, Discard this card: Look at the top two cards of your library. Put one of them into your
 * hand and the other into your graveyard.
 *
 * The discard ability is activated from hand only (ruling), the channel shape.
 */
val WakerOfWaves = card("Waker of Waves") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Whale"
    power = 7
    toughness = 7
    oracleText = "Creatures your opponents control get -1/-0.\n" +
        "{1}{U}, Discard this card: Look at the top two cards of your library. Put one of them into your hand and the other into your graveyard."

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.opponentControls()),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Patterns.Library.lookAtTopAndKeep(count = 2, keepCount = 1)
        description = "{1}{U}, Discard this card: Look at the top two cards of your library. " +
            "Put one of them into your hand and the other into your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Svetlin Velinov"
        flavorText = "On the open sea, there is nowhere to hide."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abb47990-a5a9-4a22-a8bb-d229b17132c6.jpg?1783930714"
        ruling("2020-06-23", "You can activate the last ability of Waker of Waves only if it's in your hand.")
    }
}
