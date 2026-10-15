package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Topiary Stomper
 * {1}{G}{G}
 * Creature — Plant Dinosaur
 * 4 / 4
 * Vigilance
 * When this creature enters, search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 * This creature can't attack or block unless you control seven or more lands.
 *
 * The attack/block restriction is the pair of [CantAttackUnless] / [CantBlockUnless] statics
 * sharing one "seven or more lands" condition.
 */
val TopiaryStomper = card("Topiary Stomper") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Dinosaur"
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "When this creature enters, search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\n" +
        "This creature can't attack or block unless you control seven or more lands."
    power = 4
    toughness = 4

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    staticAbility {
        ability = CantAttackUnless(Conditions.ControlLandsAtLeast(7))
    }

    staticAbility {
        ability = CantBlockUnless(Conditions.ControlLandsAtLeast(7))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87bb2699-280f-4e1e-b3f8-73efe6088f31.jpg"
    }
}
