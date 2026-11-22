package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mirko Vosk, Mind Drinker
 * {3}{U}{B}
 * Legendary Creature — Vampire
 * 2/4
 * Flying
 * Whenever Mirko Vosk deals combat damage to a player, that player reveals cards from the top of
 * their library until they reveal four land cards, then puts those cards into their graveyard.
 *
 * The damaged player is [Player.TriggeringPlayer]; `gatherUntilMatch(count = 4)` walks their
 * library until the fourth land (or the library runs out), and every revealed card — lands
 * included — goes to that player's graveyard (Consuming Aberration pattern).
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val MirkoVoskMindDrinker = card("Mirko Vosk, Mind Drinker") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Vampire"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever Mirko Vosk deals combat damage to a player, that player reveals cards from the top of their library until they reveal four land cards, then puts those cards into their graveyard."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(
                GameObjectFilter.Land,
                player = Player.TriggeringPlayer,
                count = DynamicAmounts.fixed(4)
            )
            reveal(allRevealed)
            toGraveyard(allRevealed, player = Player.TriggeringPlayer)
        }
        description = "Whenever Mirko Vosk deals combat damage to a player, that player reveals cards from the top of their library until they reveal four land cards, then puts those cards into their graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d37cdd3e-4303-4391-aff4-4a543e65a836.jpg?1783940025"
    }
}
