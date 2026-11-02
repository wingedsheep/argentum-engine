package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Doublecast
 * {R}{R}
 * Sorcery
 * When you next cast an instant or sorcery spell this turn, copy that spell. You may choose new
 * targets for the copy.
 */
val Doublecast = card("Doublecast") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "When you next cast an instant or sorcery spell this turn, copy that spell. " +
        "You may choose new targets for the copy."

    spell {
        effect = Effects.CopyNextSpellCast(
            copies = 1,
            spellFilter = GameObjectFilter.InstantOrSorcery
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Even Amundsen"
        flavorText = "First the insult, then the encore."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be8875d8-fe8c-4721-aa8d-978bf72e9c26.jpg?1783934555"
        ruling(
            "2018-07-13",
            "If you cast one Doublecast after another, you'll copy the second Doublecast. Each " +
                "resolves, creating two delayed triggered abilities. The next spell you cast after " +
                "that will be copied twice. If that spell is also a Doublecast, the following spell " +
                "will be copied three times, and so on.",
        )
        ruling(
            "2018-07-13",
            "A copy is created even if the spell that caused Doublecast's ability to trigger has " +
                "been countered by the time that ability resolves. The copy resolves before the " +
                "original spell.",
        )
        ruling(
            "2018-07-13",
            "The copy that Doublecast's ability creates is created on the stack, so it's not " +
                "\"cast.\" Abilities that trigger when a player casts a spell won't trigger.",
        )
    }
}
