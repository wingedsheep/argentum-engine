package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Carrion Grub
 * {3}{B}
 * Creature — Insect
 * 0/5
 *
 * This creature gets +X/+0, where X is the greatest power among creature cards in your graveyard.
 * When this creature enters, mill four cards.
 *
 * X is the max-power aggregate over your graveyard's creature cards (0 when there are none), read
 * continuously by a self-scoped [GrantDynamicStats]; it only applies on the battlefield.
 */
val CarrionGrub = card("Carrion Grub") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    power = 0
    toughness = 5
    oracleText = "This creature gets +X/+0, where X is the greatest power among creature cards in your graveyard.\n" +
        "When this creature enters, mill four cards. (Put the top four cards of your library into your graveyard.)"

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature).maxPower(),
            toughnessBonus = DynamicAmounts.fixed(0),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(4)
        description = "When this creature enters, mill four cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31df3d95-bbdb-449d-9601-4fa844c3c640.jpg?1783930712"
        ruling(
            "2020-06-23",
            "Carrion Grub's first ability applies only while it is on the battlefield. In all other zones, " +
                "its power is 0."
        )
        ruling(
            "2020-06-23",
            "Any counters on creatures that die or effects that affect them until end of turn won't apply to " +
                "those cards in their owner's graveyard."
        )
    }
}
