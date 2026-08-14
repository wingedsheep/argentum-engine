package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Bonehoard Dracosaur
 * {3}{R}{R}
 * Creature — Dinosaur Dragon
 * 5/5
 *
 * Flying, first strike
 * At the beginning of your upkeep, exile the top two cards of your library.
 * You may play them this turn. If you exiled a land card this way, create a 3/1 red
 * Dinosaur creature token. If you exiled a nonland card this way, create a Treasure token.
 *
 * Implementation notes:
 * - The upkeep trigger is [Patterns.Exile.impulse] over the top two cards (exile them into
 *   the shared collection "dracosaurExiled" + grant may-play until end of turn), followed by
 *   two independent [ConditionalEffect] gates that read that same collection: one checking
 *   [GameObjectFilter.Land] for the Dinosaur token, one checking [GameObjectFilter.Nonland]
 *   for the Treasure token. Both conditions can be true simultaneously if both a land and a
 *   nonland are exiled (e.g. top card is a land, second is a nonland), producing both tokens —
 *   as the oracle wording "if … this way" requires.
 * - impulse's may-play permission covers both spells and lands.
 * - The 3/1 red Dinosaur token has no imageUri because no separate token card exists
 *   in the LCI dump for this exact stats/type combination.
 */
val BonehoardDracosaur = card("Bonehoard Dracosaur") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying, first strike\n" +
        "At the beginning of your upkeep, exile the top two cards of your library. " +
        "You may play them this turn. If you exiled a land card this way, create a 3/1 red Dinosaur creature token. " +
        "If you exiled a nonland card this way, create a Treasure token."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    // At the beginning of your upkeep: impulse 2 + conditional token bonuses.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Composite(listOf(
            // Exile the top two cards; you may play them this turn (impulse draw).
            Patterns.Exile.impulse(count = 2, storeAs = "dracosaurExiled"),
            // If you exiled a land card this way, create a 3/1 red Dinosaur creature token.
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch("dracosaurExiled", GameObjectFilter.Land),
                effect = Effects.CreateToken(
                    power = 3,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Dinosaur"),
                    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee0702f9-769b-40c0-96a7-508dc8f2652c.jpg?1783913606",
                )
            ),
            // If you exiled a nonland card this way, create a Treasure token.
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch("dracosaurExiled", GameObjectFilter.Nonland),
                effect = Effects.CreateTreasure()
            )
        ))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "134"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2220ed60-3f8f-4dd2-8319-6a06896a5350.jpg?1782694501"
    }
}
