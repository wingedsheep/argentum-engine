package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Squee, Dubious Monarch
 * {2}{R}
 * Legendary Creature — Goblin Noble
 * 2/2
 * Haste
 * Whenever Squee attacks, create a 1/1 red Goblin creature token that's tapped and attacking.
 * You may cast this card from your graveyard by paying {3}{R} and exiling four other cards from
 * your graveyard rather than paying its mana cost.
 *
 * The graveyard clause is a keywordless alternative cost, not escape: {3}{R} replaces the mana
 * cost and the four *other* graveyard cards are its non-mana half. It confers no "escaped"
 * designation, and — per the rulings — a countered or dead Squee lands back in the graveyard and
 * can be cast this way again.
 */
val SqueeDubiousMonarch = card("Squee, Dubious Monarch") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Noble"
    power = 2
    toughness = 2
    oracleText = "Haste\nWhenever Squee attacks, create a 1/1 red Goblin creature token that's tapped and attacking.\n" +
        "You may cast this card from your graveyard by paying {3}{R} and exiling four other cards from your " +
        "graveyard rather than paying its mana cost."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/1/2/128511d0-bc4c-48d9-9e8e-9c7c945cfaeb.jpg?1783921128",
            tapped = true,
            attacking = true
        )
        description = "Whenever Squee attacks, create a 1/1 red Goblin creature token that's tapped and attacking."
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            alternativeManaCost = ManaCost.parse("{3}{R}"),
            additionalCost = Costs.additional.ExileOtherCards(4)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cd907ed-9c68-4bd6-af92-6244909fdf8b.jpg?1783921310"

        ruling(
            "2022-09-09",
            "Squee's controller chooses which player or planeswalker the Goblin token is attacking. It doesn't need to be the same player or planeswalker that Squee is attacking."
        )
        ruling(
            "2022-09-09",
            "The casting permission granted by Squee's last ability doesn't change when you may cast it from your graveyard."
        )
        ruling(
            "2022-09-09",
            "If Squee is countered or dies after being cast from your graveyard, it returns to the graveyard. It may be cast this way again later."
        )
    }
}
