package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Felhide Brawler
 * {1}{B}
 * Creature — Minotaur
 * 2/2
 *
 * This creature can't block unless you control another Minotaur.
 *
 * "Another" excludes the Brawler itself (it is a Minotaur too), so two Brawlers enable each
 * other. The condition is checked only as blockers are declared; the other Minotaur needn't block.
 * Same shape as Howlpack Wolf.
 */
val FelhideBrawler = card("Felhide Brawler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Minotaur"
    power = 2
    toughness = 2
    oracleText = "This creature can't block unless you control another Minotaur."

    staticAbility {
        ability = CantBlockUnless(
            Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype("Minotaur"),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Nils Hamm"
        flavorText = "Burial rites among the Felhide minotaurs involve devouring those who fell in battle, " +
            "to remove their shame from memory and to fuel the survivors' revenge."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a829bb0-6a84-46de-9c2d-14176cf542e6.jpg?1783939557"
        ruling(
            "2014-02-01",
            "Whether you control another Minotaur is checked only as you declare blockers. " +
                "The other Minotaur doesn't have to block."
        )
    }
}
