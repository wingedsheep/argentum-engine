package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aetherspouts — Magic 2015 #44
 * {3}{U}{U} · Instant
 *
 * "For each attacking creature, its owner puts it on their choice of the top or bottom of their
 * library." Each owner, active player first (the ruling), splits the attacking creatures they own
 * into a top pile and a bottom pile; each pile then goes into that owner's library in the order
 * they choose ("the owner of those cards may arrange them in any order").
 */
val Aetherspouts = card("Aetherspouts") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "For each attacking creature, its owner puts it on their choice of the top or bottom of their library."

    spell {
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.Pipeline {
                val owned = gather(
                    CardSource.BattlefieldMatching(GameObjectFilter.Creature.attacking().ownedByYou())
                )
                val (top, bottom) = chooseAnyNumberSplit(
                    owned,
                    prompt = "Choose attacking creatures you own to put on top of your library; the rest go on the bottom",
                    selectedLabel = "Top of library",
                    remainderLabel = "Bottom of library",
                    useTargetingUI = true,
                )
                toLibraryTop(top, order = CardOrder.OwnerChooses)
                toLibraryBottom(bottom, order = CardOrder.OwnerChooses)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Dan Murayama Scott"
        flavorText = "\"Don't worry, there's plenty for everyone.\"\n—Vickon, Eleventh Company battlemage"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46f1b48f-6528-46bd-a384-2358af25e500.jpg?1783939195"
    }
}
