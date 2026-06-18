package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Visionary's Dance
 * {5}{U}{R}
 * Sorcery
 *
 * Create two 3/3 blue and red Elemental creature tokens with flying.
 * {2}, Discard this card: Look at the top two cards of your library. Put one of them into your
 * hand and the other into your graveyard.
 *
 * The main spell creates two 3/3 blue-and-red flying Elementals. The card also carries a
 * hand-activated ability (CR 113.6 — usable while it sits in hand): pay {2} and discard it to
 * look at the top two and keep one in hand, the other to the graveyard — the standard
 * [Patterns.Library.lookAtTopAndKeep] recipe whose default destinations (kept → hand, rest →
 * graveyard) match the oracle text exactly.
 */
val VisionarysDance = card("Visionary's Dance") {
    manaCost = "{5}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Create two 3/3 blue and red Elemental creature tokens with flying.\n" +
        "{2}, Discard this card: Look at the top two cards of your library. Put one of them into " +
        "your hand and the other into your graveyard."

    spell {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.BLUE, Color.RED),
            creatureTypes = setOf("Elemental"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/5/7/57b98846-85e3-47c7-a903-29953d0b0e8a.jpg?1775828504"
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.DiscardSelf)
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmount.Fixed(2),
            keepCount = DynamicAmount.Fixed(1)
        )
        activateFromZone = Zone.HAND
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "Josiah \"Jo\" Cameron"
        flavorText = "Choreography powers the spell, but improvisation shapes it."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/846a0e79-a530-429e-8f7f-4b87f1b0156e.jpg?1776000377"
    }
}
