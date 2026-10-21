package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Plargg and Nassari
 * {3}{R}{R}
 * Legendary Creature — Orc Efreet
 * 5/4
 *
 * At the beginning of your upkeep, each player exiles cards from the top of their library until
 * they exile a nonland card. An opponent chooses a nonland card exiled this way. You may cast up
 * to two spells from among the other cards exiled this way without paying their mana costs.
 *
 * Only the nonland cards can be cast, so the "other cards" pool is the nonland matches minus the
 * opponent's pick; lands (and anything not cast) stay in exile.
 */
val PlarggAndNassari = card("Plargg and Nassari") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Orc Efreet"
    power = 5
    toughness = 4
    oracleText = "At the beginning of your upkeep, each player exiles cards from the top of their " +
        "library until they exile a nonland card. An opponent chooses a nonland card exiled this way. " +
        "You may cast up to two spells from among the other cards exiled this way without paying their mana costs."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val (nonlands, revealed) = gatherUntilMatch(GameObjectFilter.Nonland, player = Player.Each)
            exile(revealed)
            val (_, others) = chooseExactlySplit(
                count = 1,
                from = nonlands,
                chooser = Chooser.Opponent,
                prompt = "Choose a nonland card exiled this way",
            )
            run(Effects.CastUpToNFromCollectionWithoutPayingCost(others, maxCasts = 2))
        }
        description = "At the beginning of your upkeep, each player exiles cards from the top of their " +
            "library until they exile a nonland card. An opponent chooses a nonland card exiled this way. " +
            "You may cast up to two spells from among the other cards exiled this way without paying their mana costs."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Yigit Koroglu"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/179a0525-a142-46f6-9b5b-06a2fbb25556.jpg?1783916524"
        ruling("2023-05-12", "In a multiplayer game, you choose an opponent to choose a nonland card exiled with Plargg and Nassari.")
        ruling("2023-05-12", "In a two-player game, assuming each player eventually exiles a nonland card, your opponent will choose one of them, and you'll be able to cast the other one.")
        ruling("2023-05-12", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2023-05-12", "All the spells you cast due to the triggered ability are cast during the resolution of that ability. You can cast them in any order.")
        ruling("2023-05-12", "Cards not cast this way, including any land cards, remain in exile.")
    }
}
