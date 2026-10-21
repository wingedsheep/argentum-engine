package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reckless Handling {1}{R}
 * Sorcery
 *
 * Search your library for an artifact card, reveal it, put it into your hand, shuffle, then
 * discard a card at random. If an artifact card was discarded this way, Reckless Handling deals
 * 2 damage to each opponent.
 *
 * The tutor is the stock revealed search-to-hand; the random discard runs whether or not an
 * artifact was found (and may pick the card just fetched). The discarded card stays in its
 * pipeline collection after it moves, so "if an artifact card was discarded this way" is a
 * filtered match over that collection.
 */
val RecklessHandling = card("Reckless Handling") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Search your library for an artifact card, reveal it, put it into your hand, " +
        "shuffle, then discard a card at random. If an artifact card was discarded this way, " +
        "Reckless Handling deals 2 damage to each opponent."

    spell {
        effect = Patterns.Library.searchLibrary(filter = GameObjectFilter.Artifact, reveal = true) then
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val discarded = chooseRandom(1, from = hand)
                discard(discarded)
                run(
                    Effects.If(
                        condition = whenMatches(discarded, GameObjectFilter.Artifact),
                        then = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
                    )
                )
            }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Miguel Mercado"
        flavorText = "The relic shattered, and something in Nahiri broke as well."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/239165bb-7819-4d54-a84c-2911934253d6.jpg?1783916521"

        ruling(
            "2023-05-12",
            "You can't do anything between putting the artifact card you found in your hand and " +
                "discarding. You may end up discarding that card. But hey, if you do, 2 damage!"
        )
        ruling(
            "2023-05-12",
            "You discard a card at random even if you didn't find an artifact card in your library."
        )
    }
}
