package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Falkenrath Aristocrat
 * {2}{B}{R}
 * Creature — Vampire Noble
 * 4/1
 * Flying, haste
 * Sacrifice a creature: This creature gains indestructible until end of turn. If the sacrificed
 * creature was a Human, put a +1/+1 counter on this creature.
 *
 * Same shape as Falkenrath Torturer: the Human rider reads the cost-time snapshot of the
 * sacrificed creature via [Conditions.SacrificedHadSubtype]. "Sacrifice a creature" includes this
 * creature itself, so the cost is [Costs.Sacrifice] (not `SacrificeAnother`).
 */
val FalkenrathAristocrat = card("Falkenrath Aristocrat") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Vampire Noble"
    power = 4
    toughness = 1
    oracleText = "Flying, haste\n" +
        "Sacrifice a creature: This creature gains indestructible until end of turn. " +
        "If the sacrificed creature was a Human, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SacrificedHadSubtype("Human"),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "138"
        artist = "Igor Kieryluk"
        flavorText = "\"Your blood is quite pleasing. A pity there's not enough for both of us.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c397388e-ebe2-4034-83b7-a7c0df1af78f.jpg?1783940796"
    }
}
