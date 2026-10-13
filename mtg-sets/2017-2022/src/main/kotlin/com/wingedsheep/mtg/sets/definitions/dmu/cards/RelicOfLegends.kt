package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Relic of Legends
 * {3}
 * Artifact
 * {T}: Add one mana of any color.
 * Tap an untapped legendary creature you control: Add one mana of any color.
 */
val RelicOfLegends = card("Relic of Legends") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\nTap an untapped legendary creature you control: Add one mana of any color."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    // Not the {T} symbol, so a summoning-sick legendary creature can pay it (see ruling).
    activatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Creature.legendary())
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Titus Lunter"
        flavorText = "\"If the worst parts of history can repeat themselves, it stands to reason that the best parts can as well.\"\n—Teferi"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64a2809e-c441-416c-90ff-6fb1e246dff3.jpg?1783921269"
        ruling(
            "2022-09-09",
            "You can tap any untapped legendary creature you control, including one you haven't controlled continuously since the beginning of your most recent turn, to pay the cost of Relic of Legends's activated ability."
        )
    }
}
