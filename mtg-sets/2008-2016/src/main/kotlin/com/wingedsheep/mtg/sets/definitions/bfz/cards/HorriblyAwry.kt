package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Horribly Awry
 * {1}{U}
 * Instant
 * Devoid (This card has no color.)
 * Counter target creature spell with mana value 4 or less. If that spell is countered this way, exile it
 * instead of putting it into its owner's graveyard.
 *
 * Deny Existence's [Effects.CounterSpellToExile] on Thoughtbind's mana-value-capped target, narrowed to
 * creature spells.
 */
val HorriblyAwry = card("Horribly Awry") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Counter target creature spell with mana value 4 or less. If that spell is countered this way, exile it " +
        "instead of putting it into its owner's graveyard."

    keywords(Keyword.DEVOID)

    spell {
        target(TargetFilter.CreatureSpellOnStack.manaValueAtMost(4))
        effect = Effects.CounterSpellToExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Clint Cearley"
        flavorText = "\"We are nothing but dust to them—our bodies, our actions, even our words.\"\n—Jori En, expedition leader"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cd05532-686e-40dc-858b-8a77a3628c99.jpg?1783938213"
        ruling("2015-08-25", "The mana value of a spell doesn't change if its controller paid an alternative cost (such as an evoke cost) to cast it.")
    }
}
