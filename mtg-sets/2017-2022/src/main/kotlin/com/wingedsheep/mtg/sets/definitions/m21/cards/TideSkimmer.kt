package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tide Skimmer
 * {3}{U}
 * Creature — Drake
 * 2/3
 * Flying
 * Whenever you attack with two or more creatures with flying, draw a card.
 *
 * One declare-attackers trigger counting declared attackers with (projected) flying — it fires once
 * however many fliers attack, and the draw stands even if they lose flying afterwards (rulings).
 */
val TideSkimmer = card("Tide Skimmer") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    power = 2
    toughness = 3
    oracleText = "Flying\nWhenever you attack with two or more creatures with flying, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.attacks(
            with = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            minAttackers = 2,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Joseph Meehan"
        flavorText = "Fishing boats often follow hunting drakes to find the largest schools of fish."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dee1d595-a998-4652-931f-a1a72446f3a6.jpg?1783930717"
        ruling(
            "2020-06-23",
            "Tide Skimmer's ability triggers just once, no matter how many creatures with flying attack " +
                "beyond the second."
        )
        ruling(
            "2020-06-23",
            "You draw a card even if some or all of the attacking creatures lose flying or leave combat " +
                "before Tide Skimmer's ability resolves."
        )
    }
}
