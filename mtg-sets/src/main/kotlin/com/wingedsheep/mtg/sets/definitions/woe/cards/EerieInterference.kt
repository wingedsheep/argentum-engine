package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Eerie Interference
 * {2}{W}
 * Instant
 *
 * Prevent all damage that would be dealt to you and creatures you control this turn by creatures.
 *
 * A wider Fog: not combat-only, so it also blanks fight effects and creature-sourced ability damage
 * ("this creature deals 2 damage to any target") for the rest of the turn — but only from *creature*
 * sources, so burn spells, artifacts and planeswalkers still get through.
 *
 * Modelled with [Effects.PreventAllDamageToYouAndGroup]: one recipient-group shield covering both
 * the controller and their creatures, narrowed to creature sources. Both filters are re-evaluated
 * against projected state at the moment damage would be dealt, so a creature that changes
 * controller or is animated/de-animated mid-turn is judged as it is then, not as it was on
 * resolution.
 */
val EerieInterference = card("Eerie Interference") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all damage that would be dealt to you and creatures you control this " +
        "turn by creatures."

    spell {
        effect = Effects.PreventAllDamageToYouAndGroup(
            group = Filters.Group.creaturesYouControl,
            fromSources = Filters.Group.allCreatures,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Néstor Ossandón Leal"
        flavorText = "Ranks of sleeping Ardenvale knights filled Eriette's throne room, ready to " +
            "defend their witch-queen with unthinking devotion."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42a74545-75c8-4a6b-bee1-ac5665d9bcf0.jpg?1783915133"
    }
}
