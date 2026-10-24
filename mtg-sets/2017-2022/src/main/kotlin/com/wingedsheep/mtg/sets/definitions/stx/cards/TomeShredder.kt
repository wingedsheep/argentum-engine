package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tome Shredder
 * {2}{R}
 * Creature — Wolf
 * 2/2
 *
 * Haste
 * {T}, Exile an instant or sorcery card from your graveyard: Put a +1/+1 counter on this creature.
 */
val TomeShredder = card("Tome Shredder") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "{T}, Exile an instant or sorcery card from your graveyard: Put a +1/+1 counter on this creature."

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.ExileFromGraveyard(1, GameObjectFilter.InstantOrSorcery),
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{T}, Exile an instant or sorcery card from your graveyard: Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Sam Rowan"
        flavorText = "\"Quintorius, would you care to explain why you didn't complete your homework?\"\n" +
            "—Augusta, Lorehold dean"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7cc9ea8-db4e-4f41-800f-fb0dbcb2c345.jpg?1783927349"
    }
}
