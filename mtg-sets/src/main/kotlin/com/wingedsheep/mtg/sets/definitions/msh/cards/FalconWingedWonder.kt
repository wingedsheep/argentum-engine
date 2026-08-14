package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect

/**
 * Falcon, Winged Wonder
 * {4}{U}
 * Legendary Creature — Human Hero
 * 3/4
 *
 * Flying
 * Avian Telepathy — When Falcon enters, create Redwing, a legendary 1/1 blue Bird Scout creature
 * token with flying and "Whenever Redwing attacks, surveil 1."
 *
 * "Avian Telepathy" is an ability word — pure flavor, no rules meaning — so it only appears in the
 * oracle text. Redwing is a named token carrying its own attack trigger, so it is a registered
 * `PredefinedTokens` definition minted via [CreatePredefinedTokenEffect]; the trigger detector
 * resolves the token's surveil trigger from that definition by name.
 */
val FalconWingedWonder = card("Falcon, Winged Wonder") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Hero"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "Avian Telepathy — When Falcon enters, create Redwing, a legendary 1/1 blue Bird Scout " +
        "creature token with flying and \"Whenever Redwing attacks, surveil 1.\" (Look at the top " +
        "card of your library. You may put it into your graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreatePredefinedTokenEffect("Redwing")
        description = "Avian Telepathy — When Falcon enters, create Redwing, a legendary 1/1 blue " +
            "Bird Scout creature token with flying and \"Whenever Redwing attacks, surveil 1.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Vilhelmas Banys"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dc209ed-0d4c-4d0c-90e8-04cadc3d4c3d.jpg?1783902960"
    }
}
