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
 * Kazandu Refuge
 *
 * Land
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {R} or {G}.
 *
 * The Zendikar "Refuge" cycle is the original gainland shape that Khans of Tarkir later
 * reprinted as [JungleHollow]: an [EntersTapped] replacement effect for the printed first
 * line, a [Triggers.EntersBattlefield] trigger carrying [Effects.GainLife](1), and the dual
 * mana line spelled as **two** [Effects.AddMana] abilities sharing a [Costs.Tap] cost rather
 * than one choice effect — the majority SDK form for a plain "Add {R} or {G}." with no
 * rider on the ability. Both mana abilities are `manaAbility = true` with
 * [TimingRule.ManaAbility], so they resolve without using the stack.
 */
val KazanduRefuge = card("Kazandu Refuge") {
    manaCost = ""
    colorIdentity = "RG"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, you gain 1 life.\n" +
        "{T}: Add {R} or {G}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8af66f9c-c90b-45e0-a54c-a76e7e1b9dff.jpg"
    }
}
