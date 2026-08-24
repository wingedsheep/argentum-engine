package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * New Benalia
 *
 * Land
 *
 * This land enters tapped.
 * When this land enters, scry 1. (Look at the top card of your library. You may put that card on the bottom.)
 * {T}: Add {W}.
 *
 * The enters-tapped utility land the Theros "Temple" cycle later reprinted wholesale: an
 * [EntersTapped] replacement effect for the printed first line, a [Triggers.EntersBattlefield]
 * trigger carrying [Patterns.Library].scry(1), and one [Effects.AddMana] ability on [Costs.Tap]
 * (`manaAbility = true` with [TimingRule.ManaAbility], so it resolves without using the stack).
 * Scry is a compact SDK macro rather than a hand-rolled look-and-reorder pipeline, so the reminder
 * text needs no separate wiring.
 */
val NewBenalia = card("New Benalia") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, scry 1. (Look at the top card of your library. You may put that " +
        "card on the bottom.)\n" +
        "{T}: Add {W}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.scry(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18bde721-8fa0-4f69-9909-b4864929e676.jpg"
    }
}
