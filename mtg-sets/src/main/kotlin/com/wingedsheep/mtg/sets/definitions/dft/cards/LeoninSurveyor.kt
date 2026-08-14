package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Leonin Surveyor — Aetherdrift #18
 * {1}{W} · Creature — Cat Scout · 2/2
 *
 * Start your engines!
 * During your turn, this creature has first strike.
 * Max speed — {3}, Exile this card from your graveyard: Draw a card.
 *
 * The white member of the Surveyor cycle. "During your turn, ..." is a
 * [ConditionalStaticAbility] gated by [Conditions.IsYourTurn] over a self-scoped
 * [GrantKeyword] — the projection re-evaluates it each time the active player changes, so
 * the first strike appears and disappears on its own rather than needing an untap-step
 * trigger. The graveyard draw is the cycle's shared [maxSpeed] activated ability
 * (see [LoxodonSurveyor] for why the max-speed gate follows the ability out of play).
 */
val LeoninSurveyor = card("Leonin Surveyor") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Scout"
    power = 2
    toughness = 2
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "During your turn, this creature has first strike.\n" +
        "Max speed — {3}, Exile this card from your graveyard: Draw a card."

    startYourEngines()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{3}"), Costs.ExileSelf)
            effect = Effects.DrawCards(1)
            activateFromZone = Zone.GRAVEYARD
            description = "{3}, Exile this card from your graveyard: Draw a card."
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e08e4107-213f-491b-a032-8e3367009ba8.jpg?1783907918"
    }
}
