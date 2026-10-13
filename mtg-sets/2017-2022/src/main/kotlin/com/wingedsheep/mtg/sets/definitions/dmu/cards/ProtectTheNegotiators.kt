package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Protect the Negotiators — Dominaria United #62
 * {1}{U} · Instant
 *
 * The instructions run in printed order (2022-09-09 ruling): a kicked cast creates the Soldier
 * first, so it is already counted when the "{1} for each creature you control" tax is read.
 */
val ProtectTheNegotiators = card("Protect the Negotiators") {
    manaCost = "{1}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Kicker {W} (You may pay an additional {W} as you cast this spell.)\n" +
        "If this spell was kicked, create a 1/1 white Soldier creature token.\n" +
        "Counter target spell unless its controller pays {1} for each creature you control."

    keywordAbility(KeywordAbility.kicker("{W}"))

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.If(
            Conditions.WasKicked,
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
                imageUri = "https://cards.scryfall.io/normal/front/8/c/8c4b0257-2ca5-4015-9d63-d7cf6e87ab9d.jpg?1783921132"
            )
        ) then Effects.CounterUnlessDynamicPays(DynamicAmounts.creaturesYouControl())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb6db604-8bbe-4719-88bd-f3cad8c4725d.jpg?1783921349"
        ruling(
            "2022-09-09",
            "Once Protect the Negotiators starts to resolve, its instructions are followed in the " +
                "order they're written. If it was kicked, the Soldier creature token will be on the " +
                "battlefield when the amount of mana the target spell's controller needs to pay is determined."
        )
        ruling("2022-09-09", "You may not cast Protect the Negotiators without a spell to target.")
    }
}
