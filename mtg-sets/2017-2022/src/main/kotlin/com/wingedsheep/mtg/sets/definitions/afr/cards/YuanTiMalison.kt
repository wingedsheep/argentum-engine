package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Yuan-Ti Malison
 * {1}{U}
 * Creature — Snake Rogue
 * 2/1
 * This creature can't be blocked as long as it's attacking alone.
 * Whenever this creature deals combat damage to a player, venture into the dungeon.
 * (Enter the first room or advance to the next room.)
 *
 * The evasion is the same shape as Gutter Skulker's: a conditional [CantBeBlocked] on the source,
 * gated on the live "attacking alone" state predicate (CR 506.5).
 */
val YuanTiMalison = card("Yuan-Ti Malison") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Rogue"
    oracleText = "This creature can't be blocked as long as it's attacking alone.\n" +
        "Whenever this creature deals combat damage to a player, venture into the dungeon. " +
        "(Enter the first room or advance to the next room.)"
    power = 2
    toughness = 1

    staticAbility {
        ability = CantBeBlocked(filter = GroupFilter.source())
        condition = Conditions.SourceMatches(GameObjectFilter.Any.attackingAlone())
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Oriana Menendez"
        flavorText = "Silent. Slithering. Sinister."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f51afd9-7093-4aa3-ad5b-7383684d6285.jpg?1783926503"
    }
}
