package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Littjara Mirrorlake
 * Land
 * This land enters tapped.
 * {T}: Add {U}.
 * {2}{G}{G}{U}, {T}, Sacrifice this land: Create a token that's a copy of target creature you
 * control, except it enters with an additional +1/+1 counter on it. Activate only as a sorcery.
 *
 * One of Kaldheim's realm lands ([GatesOfIstfell]'s shape). The "enters with an additional
 * +1/+1 counter" exception is the copy's `initialCounters` ([com.wingedsheep.mtg.sets.definitions.afr.cards.OchreJelly]'s
 * shape), placed as the token enters rather than after.
 */
val LittjaraMirrorlake = card("Littjara Mirrorlake") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {U}.\n" +
        "{2}{G}{G}{U}, {T}, Sacrifice this land: Create a token that's a copy of target creature " +
        "you control, except it enters with an additional +1/+1 counter on it. Activate only as a sorcery."

    replacementEffect(EntersTapped())

    // {T}: Add {U}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE, 1)
        manaAbility = true
    }

    // {2}{G}{G}{U}, {T}, Sacrifice this land: token copy of target creature you control, +1/+1 counter.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{G}{G}{U}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            initialCounters = mapOf(CounterType.PLUS_ONE_PLUS_ONE to DynamicAmounts.fixed(1))
        )
        timing = TimingRule.SorcerySpeed
        description = "Create a token that's a copy of target creature you control, except it " +
            "enters with an additional +1/+1 counter on it. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "264"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bf2bb3e-828f-4d53-9d0a-26bbc5e7f3cc.jpg?1783928173"
    }
}
