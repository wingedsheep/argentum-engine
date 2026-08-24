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
 * Sejiri Refuge
 *
 * Land
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {W} or {U}.
 *
 * The Zendikar "Refuge" cycle is the original gainland shape that Khans of Tarkir later
 * reprinted as [JungleHollow]: an [EntersTapped] replacement effect for the printed first
 * line, a [Triggers.EntersBattlefield] trigger carrying [Effects.GainLife](1), and the dual
 * mana line spelled as **two** [Effects.AddMana] abilities sharing a [Costs.Tap] cost rather
 * than one choice effect — the majority SDK form for a plain "Add {W} or {U}." with no
 * rider on the ability. Both mana abilities are `manaAbility = true` with
 * [TimingRule.ManaAbility], so they resolve without using the stack.
 */
val SejiriRefuge = card("Sejiri Refuge") {
    manaCost = ""
    colorIdentity = "WU"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, you gain 1 life.\n" +
        "{T}: Add {W} or {U}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fd8cb0d-d651-44e7-ae14-4035b0f1aa77.jpg"
    }
}
