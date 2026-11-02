package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gruesome Menagerie (GRN 71)
 * {3}{B}{B} — Sorcery
 *
 * Choose a creature card with mana value 1 in your graveyard, then do the same for creature cards
 * with mana value 2 and 3. Return those cards to the battlefield.
 *
 * Modelling:
 *  - Choices are made on resolution (no targets). Each pick is mandatory ("choose", not "may"),
 *    and a mana value with no eligible creature card is simply skipped — `chooseExactly` auto-selects
 *    nothing when nothing matches.
 *  - All chosen cards enter simultaneously, so the three picks are folded into one collection
 *    (the graveyard pool minus what was left unchosen) before a single move — the same shape as
 *    Queen Kayla bin-Kroog.
 */
val GruesomeMenagerie = card("Gruesome Menagerie") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a creature card with mana value 1 in your graveyard, then do the same for " +
        "creature cards with mana value 2 and 3. Return those cards to the battlefield."

    spell {
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            val one = chooseExactly(
                1, from = graveyard, filter = GameObjectFilter.Creature.manaValue(1),
                prompt = "Choose a creature card with mana value 1 in your graveyard",
                selectedLabel = "Return to the battlefield",
            )
            val two = chooseExactly(
                1, from = graveyard, filter = GameObjectFilter.Creature.manaValue(2),
                prompt = "Choose a creature card with mana value 2 in your graveyard",
                selectedLabel = "Return to the battlefield",
            )
            val three = chooseExactly(
                1, from = graveyard, filter = GameObjectFilter.Creature.manaValue(3),
                prompt = "Choose a creature card with mana value 3 in your graveyard",
                selectedLabel = "Return to the battlefield",
            )

            val unchosen = exclude(exclude(exclude(graveyard, one), two), three)
            val chosen = exclude(graveyard, unchosen)
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Yeong-Hao Han"
        flavorText = "\"Variety is also the spice of death.\"\n—Cevraya, Golgari shaman"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73c0e4d9-bf0d-4e28-a647-9010701a915e.jpg?1783934175"
        ruling("2018-10-05", "Gruesome Menagerie doesn't target the cards to return. You choose them while it's resolving. No players may take actions between the time you make each choice and the time you return them to the battlefield.")
        ruling("2018-10-05", "If a card in your graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2018-10-05", "If you don't have a creature card in your graveyard with one mana value, you just continue on to the next.")
        ruling("2018-10-05", "All of the returned cards enter the battlefield at the same time.")
    }
}
