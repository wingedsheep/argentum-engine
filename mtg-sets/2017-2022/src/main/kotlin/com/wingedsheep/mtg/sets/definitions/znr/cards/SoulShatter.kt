package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Soul Shatter
 * {2}{B}
 * Instant
 * Each opponent sacrifices a creature or planeswalker with the greatest mana value among
 * creatures and planeswalkers they control.
 *
 * Each opponent gathers their creatures and planeswalkers, keeps the ones tied for the greatest
 * mana value (a face-down permanent counts as 0), and picks one of those to sacrifice — the same
 * shape as Crackling Doom's greatest-power edict.
 */
val SoulShatter = card("Soul Shatter") {
    manaCost = "{2}{B}"
    typeLine = "Instant"
    oracleText = "Each opponent sacrifices a creature or planeswalker with the greatest mana value among creatures and planeswalkers they control."

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            Effects.Pipeline {
                val candidates = gather(
                    CardSource.ControlledPermanents(Player.You, GameObjectFilter.CreatureOrPlaneswalker)
                )
                val greatest = filter(candidates, CollectionFilter.GreatestManaValue)
                val toSacrifice = chooseExactly(
                    1,
                    from = greatest,
                    chooser = Chooser.Controller,
                    prompt = "Choose a creature or planeswalker with the greatest mana value to sacrifice",
                    useTargetingUI = true
                )
                sacrifice(toSacrifice)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Wylie Beckert"
        flavorText = "The Kargan tribes share legends of the victory their ancestors won over the Akoum Skyclave, and its horrendous cost."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c2fb07b-0f70-403b-be8b-b5217f12e671.jpg?1783929366"
        ruling("2020-09-25", "As Soul Shatter resolves, first the opponent whose turn it is (or the next opponent in turn order, if it's your turn) chooses a creature or planeswalker they control with the highest mana value among that player's creatures and planeswalkers, then each other opponent in turn order does the same, knowing the choices made before them. Then all of the chosen permanents are sacrificed at the same time.")
    }
}
