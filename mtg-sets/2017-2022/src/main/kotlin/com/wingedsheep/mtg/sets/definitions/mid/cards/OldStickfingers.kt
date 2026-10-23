package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Old Stickfingers
 * {X}{B}{G}
 * Legendary Creature — Horror
 * ✶/✶
 *
 * When you cast this spell, reveal cards from the top of your library until you reveal X
 * creature cards. Put all creature cards revealed this way into your graveyard, then put the
 * rest on the bottom of your library in a random order.
 * Old Stickfingers's power and toughness are each equal to the number of creature cards in your
 * graveyard.
 *
 * The cast trigger (`Triggers.self.isCast()`) reads the spell's announced X through
 * [DynamicAmounts.xValueOfTriggeringSpell] — inside a trigger `DynamicAmount.XValue` is the
 * trigger's own X, which a cast trigger never has (Photon Blast Barrage pattern). The trigger
 * resolves before the creature spell and keeps that X even if the spell is countered in response.
 * The reveal is GatherUntilMatch with a dynamic count (The Ring Goes South pattern); X = 0 walks
 * nothing. The characteristic-defining P/T is [dynamicStats], which works in every zone.
 */
val OldStickfingers = card("Old Stickfingers") {
    manaCost = "{X}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Horror"
    oracleText = "When you cast this spell, reveal cards from the top of your library until you reveal X " +
        "creature cards. Put all creature cards revealed this way into your graveyard, then put the rest " +
        "on the bottom of your library in a random order.\n" +
        "Old Stickfingers's power and toughness are each equal to the number of creature cards in your graveyard."

    dynamicStats(
        DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)
    )

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(
                GameObjectFilter.Creature,
                player = Player.You,
                count = DynamicAmounts.xValueOfTriggeringSpell()
            )
            reveal(allRevealed)
            move(
                allRevealed,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                filter = GameObjectFilter.Creature
            )
            move(
                allRevealed,
                CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom),
                order = CardOrder.Random,
                filter = GameObjectFilter.Noncreature
            )
        }
        description = "When you cast this spell, reveal cards from the top of your library until you reveal X " +
            "creature cards. Put all creature cards revealed this way into your graveyard, then put the rest " +
            "on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b203be6f-c565-4d8f-b200-b537569a0292.jpg?1783925557"
        ruling("2021-09-24", "The triggered ability will resolve before Old Stickfingers does. If Old Stickfingers is countered in response to its own triggered ability, you will still follow all the instructions of the triggered ability, using the chosen value of X.")
        ruling("2021-09-24", "The ability that defines the power and toughness of Old Stickfingers functions in all zones, not only while Old Stickfingers is on the battlefield. While Old Stickfingers is in a graveyard, the ability will count Old Stickfingers itself, presumably using a single, withered stickfinger.")
    }
}
