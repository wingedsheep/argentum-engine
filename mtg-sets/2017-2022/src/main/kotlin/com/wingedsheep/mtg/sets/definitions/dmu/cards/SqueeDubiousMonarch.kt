package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

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
 * The graveyard clause is escape's own wording without the keyword, so it rides the escape
 * graveyard-alternative-cost rail ([KeywordAbility.escape]): sorcery timing, {3}{R} replacing the
 * mana cost, four *other* graveyard cards exiled as the non-mana half, and — per the rulings — a
 * countered or dead Squee lands back in the graveyard and can be cast this way again. The only
 * thing escape adds is the "escaped" designation, which nothing on Squee reads.
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

    keywordAbility(KeywordAbility.escape("{3}{R}", Costs.additional.ExileOtherCards(4)))

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
