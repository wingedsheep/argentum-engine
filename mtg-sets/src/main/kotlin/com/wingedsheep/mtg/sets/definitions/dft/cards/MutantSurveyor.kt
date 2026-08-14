package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mutant Surveyor — Aetherdrift #98
 * {2}{B} · Creature — Mutant Scout · 2/3
 *
 * Start your engines!
 * {2}: This creature gets +1/+1 until end of turn.
 * Max speed — {3}, Exile this card from your graveyard: Draw a card.
 *
 * The black member of the Surveyor cycle. The pump is an ordinary generic-cost
 * self-[Effects.ModifyStats]; the graveyard draw is the cycle's shared [maxSpeed] activated
 * ability (see [LoxodonSurveyor] for why the max-speed gate follows it into the graveyard).
 */
val MutantSurveyor = card("Mutant Surveyor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Mutant Scout"
    power = 2
    toughness = 3
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "{2}: This creature gets +1/+1 until end of turn.\n" +
        "Max speed — {3}, Exile this card from your graveyard: Draw a card."

    startYourEngines()

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ModifyStats(power = 1, toughness = 1, target = EffectTarget.Self)
        description = "{2}: This creature gets +1/+1 until end of turn."
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
        collectorNumber = "98"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cec5105-3907-40d2-8e46-95acfaaaa0cc.jpg?1783907891"
    }
}
