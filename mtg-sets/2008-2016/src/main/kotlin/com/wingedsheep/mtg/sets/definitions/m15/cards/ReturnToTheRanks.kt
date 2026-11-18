package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Return to the Ranks — Magic 2015 #29 (canonical printing)
 * {X}{W}{W} · Sorcery
 *
 * Convoke
 * Return X target creature cards with mana value 2 or less from your graveyard to the battlefield.
 *
 * "X target creature cards" is exactly X (`targets(…, exactly = X)`, the Word of Binding / Icy Blast
 * shape): the announced X is both floor and cap, and the legal-action X ceiling is capped at the
 * number of legal graveyard targets. Convoke pays toward the total cost, X included, without
 * changing the announced X.
 */
val ReturnToTheRanks = card("Return to the Ranks") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText =
        "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Return X target creature cards with mana value 2 or less from your graveyard to the battlefield."

    keywords(Keyword.CONVOKE)

    spell {
        targets(
            TargetFilter.CreatureInYourGraveyard.manaValueAtMost(2),
            exactly = DynamicAmounts.xValue(),
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fccce64-abac-4b90-bbe5-dbba8434b3b4.jpg?1783939198"
        ruling(
            "2024-01-12",
            "When using convoke to cast a spell with {X} in its mana cost, first choose the value for X. " +
                "That choice, plus any cost increases or decreases, will determine the spell's total cost. " +
                "Then you can tap creatures you control to help pay that cost."
        )
    }
}
