package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lich's Mastery {3}{B}{B}{B}
 * Legendary Enchantment
 *
 * Hexproof
 * You can't lose the game.
 * Whenever you gain life, draw that many cards.
 * Whenever you lose life, for each 1 life you lost, exile a permanent you control
 * or a card from your hand or graveyard.
 * When Lich's Mastery leaves the battlefield, you lose the game.
 */
val LichsMastery = card("Lich's Mastery") {
    manaCost = "{3}{B}{B}{B}"
    typeLine = "Legendary Enchantment"
    oracleText = "Hexproof\n" +
        "You can't lose the game.\n" +
        "Whenever you gain life, draw that many cards.\n" +
        "Whenever you lose life, for each 1 life you lost, exile a permanent you control or a card from your hand or graveyard.\n" +
        "When Lich's Mastery leaves the battlefield, you lose the game."

    // Hexproof
    keywords(Keyword.HEXPROOF)

    // You can't lose the game
    staticAbility {
        ability = GrantCantLoseGame
    }

    // Whenever you gain life, draw that many cards
    triggeredAbility {
        trigger = Triggers.YouGainLife
        effect = Effects.DrawCards(DynamicAmount.TriggerLifeGainAmount)
    }

    // Whenever you lose life, for each 1 life you lost, exile a permanent you control
    // or a card from your hand or graveyard
    triggeredAbility {
        trigger = Triggers.YouLoseLife
        effect = Effects.ForceExileMultiZone(DynamicAmount.TriggerLifeLossAmount)
    }

    // When Lich's Mastery leaves the battlefield, you lose the game
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.LoseGame()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77f4c9cb-f364-4556-8673-4b19d52a2cff.jpg?1562738043"
        ruling("2018-04-27", "While you can't lose the game, your opponents can still win the game if an effect says so.")
        ruling("2018-04-27", "While you control Lich's Mastery, your life total still changes. Lich's Mastery's effects don't replace the life gain or life loss.")
        ruling("2018-04-27", "You don't have to exile all the cards from one place.")
        ruling("2018-04-27", "If you run out of other permanents, cards in hand, and cards in graveyard, you'll have to exile Lich's Mastery itself and lose the game.")
    }
}
