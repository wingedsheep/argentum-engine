package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn

/**
 * Archon of Emeria
 * {2}{W}
 * Creature — Archon
 * 2/3
 * Flying
 * Each player can't cast more than one spell each turn.
 * Nonbasic lands your opponents control enter tapped.
 *
 * The spell cap is the global form of [RestrictSpellsCastPerTurn] (as on Eidolon of Rhetoric); the
 * engine reads each player's whole-turn spells-cast tally, so the Archon itself counts. The second
 * ability is the group [PermanentsEnterTapped] replacement (as on Thalia, Heretic Cathar), which
 * overrides "enters tapped unless …" lands.
 */
val ArchonOfEmeria = card("Archon of Emeria") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Archon"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Each player can't cast more than one spell each turn.\n" +
        "Nonbasic lands your opponents control enter tapped."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = RestrictSpellsCastPerTurn(maxPerTurn = 1, eachPlayer = true)
    }

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.NonbasicLand.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Ryan Pancoast"
        flavorText = "\"Archons dispense justice according to an ancient, and flawed, premise.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/228c1650-da3c-4099-91b6-18e3873c9cdb.jpg?1783929424"
        ruling(
            "2020-09-25",
            "Archon of Emeria looks at the entire turn to see if a player has cast a spell, even if " +
                "Archon of Emeria wasn't on the battlefield when that spell was cast. Notably, you can't " +
                "cast Archon of Emeria and then cast another spell during the same turn."
        )
        ruling(
            "2020-09-25",
            "If you cast a spell that was countered, you can't cast another spell during the same turn."
        )
        ruling(
            "2020-09-25",
            "If an effect says that a land enters tapped unless a condition is met or a cost is paid, " +
                "that land enters tapped even if that condition is met or that cost is paid if it's " +
                "affected by Archon of Emeria's second ability."
        )
    }
}
