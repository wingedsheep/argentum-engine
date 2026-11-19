package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Worst Fears
 * {7}{B}
 * Sorcery
 * You control target player during that player's next turn. Exile Worst Fears. (You see all cards
 * that player could see and make all decisions for that player.)
 *
 * Mindslaver as a sorcery: [Effects.HijackNextTurn] over any target player (yourself included — a
 * ruling notes that only matters to overwrite someone else's control effect), and [selfExile] sends
 * the card to exile instead of the graveyard as it resolves, like Secret of Bloodbending.
 */
val WorstFears = card("Worst Fears") {
    manaCost = "{7}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You control target player during that player's next turn. Exile Worst Fears. " +
        "(You see all cards that player could see and make all decisions for that player.)"

    spell {
        val player = target(Targets.Player)
        selfExile()
        effect = Effects.HijackNextTurn(player)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "87"
        artist = "Eric Deschamps"
        flavorText = "Elspeth feared that her trespass into Nyx would not go unanswered."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f4e21ac-4f3e-4a3e-83c0-0a59a1b84020.jpg?1783939427"

        ruling("2014-04-26", "The player you're controlling is still the active player during that turn.")
        ruling("2014-04-26", "You only control the player. You don't control any of their permanents, spells, or abilities.")
        ruling("2014-04-26", "If the target player skips their next turn, you'll control the next turn the affected player actually takes.")
        ruling("2014-04-26", "Multiple player-controlling effects that affect the same player overwrite each other. The last one to be created is the one that works.")
        ruling("2014-04-26", "You could gain control of yourself using Worst Fears, but unless you do so to overwrite someone else's player-controlling effect, this doesn't do anything.")
        ruling("2014-04-26", "You can use only the affected player's resources (cards, mana, and so on) to pay costs for that player; you can't use your own. Similarly, you can use the affected player's resources only to pay that player's costs; you can't spend them on your costs.")
    }
}
