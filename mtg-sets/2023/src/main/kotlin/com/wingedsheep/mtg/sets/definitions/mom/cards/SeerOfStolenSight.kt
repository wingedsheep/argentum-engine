package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Seer of Stolen Sight
 * {2}{B}
 * Creature — Phyrexian Warlock
 * 2/3
 * Menace
 * Whenever one or more artifacts and/or creatures you control are put into a graveyard from the
 * battlefield, surveil 1.
 *
 * The batched death trigger with a creature-or-artifact filter: once per simultaneous batch, and a
 * dying noncreature artifact token (a Treasure, an Incubator) counts.
 */
val SeerOfStolenSight = card("Seer of Stolen Sight") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Warlock"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever one or more artifacts and/or creatures you control are put into a graveyard from the " +
        "battlefield, surveil 1. (Look at the top card of your library. You may put that card into your graveyard.)"
    power = 2
    toughness = 3
    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.CreatureOrArtifact.youControl()).die()
        effect = Patterns.Library.surveil(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "330"
        artist = "Betty Jiang"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d0b3232-44ee-481d-81d9-46432f853a7d.jpg?1783916902"
    }
}
