package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sphinx's Decree
 * {1}{W}
 * Sorcery
 * Each opponent can't cast instant or sorcery spells during that player's next turn.
 *
 * The ban lands on each opponent with [Duration.DuringAffectedPlayersNextTurn]: dormant for the rest
 * of this turn, binding through that opponent's own next turn, gone at its cleanup.
 */
val SphinxsDecree = card("Sphinx's Decree") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each opponent can't cast instant or sorcery spells during that player's next turn."

    spell {
        effect = Effects.CantCastSpells(
            EffectTarget.PlayerRef(Player.EachOpponent),
            Duration.DuringAffectedPlayersNextTurn,
            GameObjectFilter.InstantOrSorcery
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Daarken"
        flavorText = "\"Be still.\"\n—Azor"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5058a729-9739-4548-b420-deebbc0950b7.jpg?1783935331"
    }
}
