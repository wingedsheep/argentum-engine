package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Imperial Recruiter
 * {2}{R}
 * Creature — Human Advisor
 * 1 / 1
 *
 * When this creature enters, search your library for a creature card with power 2 or less,
 * reveal it, put it into your hand, then shuffle.
 */
val ImperialRecruiter = card("Imperial Recruiter") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, search your library for a creature card with power 2 or less, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.powerAtMost(2),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Mitsuaki Sagiri"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c473253-3992-4cc1-8b46-5d1da308c537.jpg"
    }
}
