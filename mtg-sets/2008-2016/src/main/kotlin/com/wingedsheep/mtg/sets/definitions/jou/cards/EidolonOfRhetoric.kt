package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn

/**
 * Eidolon of Rhetoric
 * {2}{W}
 * Enchantment Creature — Spirit
 * 1/4
 * Each player can't cast more than one spell each turn.
 *
 * The global (`eachPlayer = true`) form of [RestrictSpellsCastPerTurn], as on Rule of Law. The
 * engine reads each player's spells-cast-this-turn tally at cast-legality time, so spells cast
 * before the Eidolon entered — including the Eidolon itself — count toward the cap.
 */
val EidolonOfRhetoric = card("Eidolon of Rhetoric") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Spirit"
    oracleText = "Each player can't cast more than one spell each turn."
    power = 1
    toughness = 4

    staticAbility {
        ability = RestrictSpellsCastPerTurn(maxPerTurn = 1, eachPlayer = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Ryan Yee"
        flavorText = "It is the soul of a philosopher who died of starvation contemplating the universe."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3bc8b9e-4d22-41ba-b593-d383fd301ef9.jpg?1783939461"
        ruling(
            "2014-04-26",
            "Eidolon of Rhetoric will look at the entire turn to see if a player has cast a spell yet " +
                "that turn, even if Eidolon of Rhetoric wasn't on the battlefield when that spell was cast. " +
                "Specifically, you can't cast Eidolon of Rhetoric and then cast another spell that turn."
        )
    }
}
