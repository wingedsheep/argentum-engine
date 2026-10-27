package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baloth Prime — Edge of Eternities Commander #13 (canonical printing)
 * {3}{G} · Creature — Beast · 10/10
 *
 * This creature enters tapped with six stun counters on it. (If a permanent with a stun counter
 * would become untapped, remove one from it instead.)
 * Whenever you sacrifice a land, create a tapped 4/4 green Beast creature token and untap this
 * creature.
 * {4}, Sacrifice a land: You gain 2 life.
 *
 * The land-sacrifice trigger is per land (CR 603.2c), so sacrificing several lands at once
 * triggers once for each — including when Baloth Prime leaves alongside them.
 */
val BalothPrime = card("Baloth Prime") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 10
    toughness = 10
    oracleText = "This creature enters tapped with six stun counters on it. (If a permanent with a stun " +
        "counter would become untapped, remove one from it instead.)\n" +
        "Whenever you sacrifice a land, create a tapped 4/4 green Beast creature token and untap this creature.\n" +
        "{4}, Sacrifice a land: You gain 2 life."

    replacementEffect(EntersTapped())
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.STUN,
            count = 6,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Land)
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/e/b/eb243b20-85e6-4eba-9403-42272975e8a5.jpg?1783905790"
        ) then Effects.Untap(EffectTarget.Self)
        description = "Whenever you sacrifice a land, create a tapped 4/4 green Beast creature token and untap this creature."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Sacrifice(GameObjectFilter.Land))
        effect = Effects.GainLife(2)
        description = "{4}, Sacrifice a land: You gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c723fc9-d5c9-4126-a9a6-f80c247a4b6b.jpg?1783906064"

        ruling(
            "2025-07-25",
            "If Baloth Prime leaves the battlefield at the same time as you sacrifice one or more lands " +
                "(probably because you also sacrificed Baloth Prime), its second ability triggers for each of those lands."
        )
    }
}
