package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Boosted Sloop
 * {1}{U}{R}
 * Artifact — Vehicle
 * 3/3
 * Menace
 * Whenever you attack, draw a card, then discard a card.
 * Crew 1
 *
 * The attack trigger is the once-per-combat group trigger ([Triggers.YouAttack]), not an
 * "attacks" trigger on this permanent — per the Scryfall ruling it fires whenever you declare
 * any attacker, even when this Vehicle isn't among them (or isn't a creature at all).
 */
val BoostedSloop = card("Boosted Sloop") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Artifact — Vehicle"
    oracleText = "Menace\nWhenever you attack, draw a card, then discard a card.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This " +
        "Vehicle becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 3
    keywords(Keyword.MENACE)
    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = Patterns.Hand.loot()
    }
    keywordAbility(KeywordAbility.crew(1))
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3563cb48-9dcb-4b92-af3a-4793adf03125.jpg?1783907863"
        ruling(
            "2025-02-07",
            "Attacking with any creature will cause the second ability to trigger, even if this " +
                "Vehicle isn't attacking."
        )
    }
}
