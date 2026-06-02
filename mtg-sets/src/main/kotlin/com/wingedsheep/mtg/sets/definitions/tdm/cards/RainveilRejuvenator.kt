package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Rainveil Rejuvenator — Tarkir: Dragonstorm #152
 * {3}{G} · Creature — Elephant Druid · 2/4
 *
 * When this creature enters, you may mill three cards.
 * {T}: Add an amount of {G} equal to this creature's power.
 *
 * The mana ability reads the creature's current power at activation
 * ([DynamicAmounts.sourcePower]); it is a mana ability (no stack, can't be responded to).
 */
val RainveilRejuvenator = card("Rainveil Rejuvenator") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant Druid"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, you may mill three cards. " +
        "(You may put the top three cards of your library into your graveyard.)\n" +
        "{T}: Add an amount of {G} equal to this creature's power."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            LibraryPatterns.mill(3),
            descriptionOverride = "You may mill three cards."
        )
        description = "When this creature enters, you may mill three cards."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, DynamicAmounts.sourcePower())
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bc5c316-6a41-48ba-864b-da3030dd3e0e.jpg?1743204573"
    }
}
