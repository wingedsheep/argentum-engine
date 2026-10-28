package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mental Misstep — New Phyrexia #38 (canonical / earliest real printing, 2011)
 * {U/P} · Instant
 *
 * ({U/P} can be paid with either {U} or 2 life.)
 * Counter target spell with mana value 1.
 *
 * Minor Misstep's shape with an exact mana-value match instead of "1 or less". The Phyrexian pip
 * is parsed directly by `ManaCost.parse` (as on Gitaxian Probe) and counts 1 toward mana value.
 */
val MentalMisstep = card("Mental Misstep") {
    manaCost = "{U/P}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell with mana value 1."

    spell {
        target(TargetFilter.SpellOnStack.manaValue(1))
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Erica Yang"
        flavorText = "\"Your first mistake was thinking I would let you live long enough to make a " +
            "second.\"\n—Sarnvax, Gitaxian sective"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61e9c6df-1c84-4eab-9076-a4feb6347c10.jpg?1783941319"
        ruling(
            "2011-06-01",
            "Each Phyrexian mana symbol in a spell's mana cost contributes 1 to that spell's mana " +
                "value. For example, Mental Misstep's mana value is 1, regardless of how its cost was paid."
        )
        ruling("2011-06-01", "If you're at 1 life or less, you can't pay 2 life.")
        ruling(
            "2011-06-01",
            "If a spell has {X} in its mana cost, X is considered to be the value chosen for it while " +
                "that spell is on the stack."
        )
    }
}
