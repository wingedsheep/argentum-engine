package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * 70,000 Light-Years from Home
 * {1}{W}
 * Sorcery
 *
 * Exile target creature or Spacecraft. Its controller may search their library for a basic land
 * card, reveal it, put it into their hand, then shuffle.
 *
 * Path to Exile's shape: "its controller" is [Player.ControllerOf] the exiled permanent (read from
 * last-known information), and [Effects.ForEachPlayer] rebinds `Player.You` to that player so the
 * "may" and the search are theirs. Declining the search skips the shuffle too.
 */
val SeventyThousandLightYearsFromHome = card("70,000 Light-Years from Home") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target creature or Spacecraft. Its controller may search their library for a " +
        "basic land card, reveal it, put it into their hand, then shuffle."

    spell {
        val target = target(
            TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Permanent.withSubtype("Spacecraft"))
        )
        effect = Effects.Exile(target) then
            Effects.ForEachPlayer(
                Player.ControllerOf("target creature or Spacecraft"),
                listOf(
                    Effects.May(
                        Patterns.Library.searchLibrary(
                            filter = GameObjectFilter.BasicLand,
                            count = 1,
                            destination = SearchDestination.HAND,
                            reveal = true
                        )
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Ariel Orea"
        flavorText = "\"If these sensors are working . . . we're on the other side of the Galaxy.\"\n—Ensign Harry Kim"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02080f42-863a-471b-992b-67e35ef1b7b7.jpg"
    }
}
