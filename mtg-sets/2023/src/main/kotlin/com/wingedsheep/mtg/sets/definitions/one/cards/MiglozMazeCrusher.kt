package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Migloz, Maze Crusher
 * {1}{R}{G}
 * Legendary Creature — Phyrexian Beast
 * 4/4
 *
 * Migloz enters with five oil counters on it.
 * {1}, Remove an oil counter from Migloz: It gains vigilance and menace until end of turn.
 * {2}, Remove two oil counters from Migloz: It gets +2/+2 until end of turn.
 * {3}, Remove three oil counters from Migloz: Destroy target artifact or enchantment.
 */
val MiglozMazeCrusher = card("Migloz, Maze Crusher") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Phyrexian Beast"
    power = 4
    toughness = 4
    oracleText = "Migloz enters with five oil counters on it.\n" +
        "{1}, Remove an oil counter from Migloz: It gains vigilance and menace until end of turn.\n" +
        "{2}, Remove two oil counters from Migloz: It gets +2/+2 until end of turn.\n" +
        "{3}, Remove three oil counters from Migloz: Destroy target artifact or enchantment."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 5, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.RemoveCounterFromSelf(CounterType.OIL, 1))
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
        description = "{1}, Remove an oil counter from Migloz: It gains vigilance and menace until end of turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.RemoveCounterFromSelf(CounterType.OIL, 2))
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "{2}, Remove two oil counters from Migloz: It gets +2/+2 until end of turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.RemoveCounterFromSelf(CounterType.OIL, 3))
        val permanent = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(permanent)
        description = "{3}, Remove three oil counters from Migloz: Destroy target artifact or enchantment."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1171899-07d8-4e60-a79b-f162f59dc3ce.jpg?1783917999"
    }
}
