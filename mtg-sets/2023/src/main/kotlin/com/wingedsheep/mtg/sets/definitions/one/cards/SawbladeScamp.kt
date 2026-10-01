package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sawblade Scamp
 * {R}
 * Creature — Phyrexian Beast
 * 1/1
 *
 * Haste
 * Whenever you cast a noncreature spell, put an oil counter on this creature.
 * {T}, Remove an oil counter from this creature: It deals 1 damage to each opponent.
 */
val SawbladeScamp = card("Sawblade Scamp") {
    manaCost = "{R}"
    typeLine = "Creature — Phyrexian Beast"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "Whenever you cast a noncreature spell, put an oil counter on this creature.\n" +
        "{T}, Remove an oil counter from this creature: It deals 1 damage to each opponent."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.OIL, 1))
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "{T}, Remove an oil counter from this creature: It deals 1 damage to each opponent."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Helge C. Balzer"
        flavorText = "Sometimes the Great Work requires destruction to allow for more efficient reconstruction."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f04c2e5-9100-439a-9aa4-28d388518e4a.jpg?1783918025"
    }
}
