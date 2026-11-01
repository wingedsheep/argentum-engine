package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Awaken the Erstwhile — Ravnica Allegiance #61
 * {3}{B}{B} · Sorcery
 *
 * Each player discards all the cards in their hand, then creates that many 2/2 black Zombie
 * creature tokens.
 *
 * One [Effects.ForEachPlayer] over [Player.Each] whose body is the Tolarian Winds pipeline:
 * inside the loop "you" is the iterated player, so each player gathers and discards their own
 * hand and creates Zombies equal to *their* discard count (the 2019-01-25 ruling), not the total.
 */
val AwakenTheErstwhile = card("Awaken the Erstwhile") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player discards all the cards in their hand, then creates that many 2/2 " +
        "black Zombie creature tokens."

    spell {
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.Pipeline {
                val discarded = gather(CardSource.FromZone(Zone.HAND, Player.You))
                move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard)
                run(
                    Effects.CreateToken(
                        count = discarded.count,
                        power = 2,
                        toughness = 2,
                        colors = setOf(Color.BLACK),
                        creatureTypes = setOf("Zombie"),
                        controller = EffectTarget.Controller,
                        imageUri = "https://cards.scryfall.io/normal/front/2/7/27b69e52-f394-4863-943b-239e96b5cc95.jpg?1783933604"
                    )
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Jason A. Engle"
        flavorText = "In preparation for the conflict all foresaw, the Golgari called upon an army that " +
            "had slept for millennia beneath the city."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea166114-2f9b-4ca6-b573-1f49f7485580.jpg?1783933700"
        ruling("2019-01-25", "The number of Zombie tokens each player creates is equal to the number of cards that player discarded, not the total number of cards discarded.")
    }
}
