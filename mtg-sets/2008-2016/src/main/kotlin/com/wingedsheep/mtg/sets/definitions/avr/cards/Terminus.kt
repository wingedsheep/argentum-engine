package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Terminus
 * {4}{W}{W}
 * Sorcery
 *
 * Put all creatures on the bottom of their owners' libraries.
 * Miracle {W} (You may cast this card for its miracle cost when you draw it if it's the first card you drew this turn.)
 *
 * Same group-to-library shape as Aetherspouts: one pass per owner, active player first, each
 * putting the creatures they own on the bottom of their library in an order they choose (the
 * 2017-03-14 ruling).
 */
val Terminus = card("Terminus") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put all creatures on the bottom of their owners' libraries.\n" +
        "Miracle {W} (You may cast this card for its miracle cost when you draw it if it's the first " +
        "card you drew this turn.)"

    spell {
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.Pipeline(
                descriptionOverride = "put all creatures on the bottom of their owners' libraries"
            ) {
                val owned = gather(CardSource.BattlefieldMatching(GameObjectFilter.Creature.ownedByYou()))
                toLibraryBottom(owned, order = CardOrder.OwnerChooses)
            }
        )
    }

    keywordAbility(KeywordAbility.miracle("{W}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0982ea7e-05a4-4e40-98ab-ea9aa6c7342e.jpg?1783940727"
        ruling("2017-03-14", "Each player chooses the order that creatures they own are put onto the bottom of their library. This order isn't revealed to other players.")
    }
}
