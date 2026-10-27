package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bretagard Stronghold
 * Land
 * This land enters tapped.
 * {T}: Add {G}.
 * {G}{W}{W}, {T}, Sacrifice this land: Put a +1/+1 counter on each of up to two target creatures
 * you control. They gain vigilance and lifelink until end of turn. Activate only as a sorcery.
 *
 * One of Kaldheim's realm lands. The "each of up to two targets" shape is an optional `count = 2`
 * requirement fanned out by [Effects.ForEachTarget], so zero, one, or two creatures can be chosen
 * and each gets its counter and both keywords independently.
 */
val BretagardStronghold = card("Bretagard Stronghold") {
    manaCost = ""
    colorIdentity = "GW"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {G}.\n" +
        "{G}{W}{W}, {T}, Sacrifice this land: Put a +1/+1 counter on each of up to two target " +
        "creatures you control. They gain vigilance and lifelink until end of turn. " +
        "Activate only as a sorcery."

    replacementEffect(EntersTapped())

    // {T}: Add {G}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, 1)
        manaAbility = true
    }

    // {G}{W}{W}, {T}, Sacrifice this land: counters + vigilance and lifelink on up to two creatures.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}{W}{W}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        targets(TargetFilter.CreatureYouControl, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.ContextTarget(0), Duration.EndOfTurn)
        )
        timing = TimingRule.SorcerySpeed
        description = "Put a +1/+1 counter on each of up to two target creatures you control. " +
            "They gain vigilance and lifelink until end of turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24a0f2e2-cfd7-4374-a172-4357d5de47bb.jpg?1783928179"
    }
}
