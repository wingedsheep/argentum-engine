package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Jwar Isle Refuge
 *
 * Land
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {U} or {B}.
 *
 * The Zendikar "Refuge" cycle is the original gainland shape that Khans of Tarkir later
 * reprinted as [JungleHollow]: an [EntersTapped] replacement effect for the printed first
 * line, a [Triggers.EntersBattlefield] trigger carrying [Effects.GainLife](1), and the dual
 * mana line spelled as **two** [Effects.AddMana] abilities sharing a [Costs.Tap] cost rather
 * than one choice effect — the majority SDK form for a plain "Add {U} or {B}." with no
 * rider on the ability. Both mana abilities are `manaAbility = true` with
 * [TimingRule.ManaAbility], so they resolve without using the stack.
 */
val JwarIsleRefuge = card("Jwar Isle Refuge") {
    manaCost = ""
    colorIdentity = "UB"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, you gain 1 life.\n" +
        "{T}: Add {U} or {B}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Cyril Van Der Haegen"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddd9463f-d029-459d-a933-432b7bbd7a41.jpg"
    }
}
