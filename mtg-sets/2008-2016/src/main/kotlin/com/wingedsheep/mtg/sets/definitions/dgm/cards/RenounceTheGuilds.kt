package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Renounce the Guilds — Dragon's Maze #5
 * {1}{W} · Instant
 *
 * Each player sacrifices a multicolored permanent of their choice.
 *
 * The symmetric edict shape (Barter in Blood) over [Player.Each] with a multicolored filter.
 * Nothing is targeted, so the spell is castable when no player controls a multicolored
 * permanent. Players choose in APNAP order, then all chosen permanents are sacrificed together.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val RenounceTheGuilds = card("Renounce the Guilds") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Each player sacrifices a multicolored permanent of their choice."

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Multicolored,
            count = 1,
            target = EffectTarget.PlayerRef(Player.Each)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Daarken"
        flavorText = "\"Scores will die in the name of peace. That is what you call compromise?\"\n" +
            "—Gideon Jura, to Aurelia"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc9acc14-24e0-4c03-a09a-2afee351f2cc.jpg?1783940045"

        ruling(
            "2013-04-15",
            "The active player (the player whose turn it is) chooses which multicolored permanent " +
                "will be sacrificed, then each other player in turn order does the same. Then all " +
                "chosen permanents are sacrificed at the same time."
        )
        ruling(
            "2013-04-15",
            "Renounce the Guilds doesn't target any player and may be cast even if a player " +
                "doesn't control a multicolored permanent."
        )
    }
}
