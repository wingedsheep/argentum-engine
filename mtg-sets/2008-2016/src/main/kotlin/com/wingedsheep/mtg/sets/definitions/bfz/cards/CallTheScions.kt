package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Call the Scions
 * {2}{G}
 * Sorcery
 * Devoid (This card has no color.)
 * Create two 1/1 colorless Eldrazi Scion creature tokens. They have "Sacrifice this token: Add {C}."
 *
 * The Scion token is the same inline shape as Brood Monitor's.
 */
val CallTheScions = card("Call the Scions") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Create two 1/1 colorless Eldrazi Scion creature tokens. " +
        "They have \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            count = 2,
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.SacrificeSelf,
                    effect = Effects.AddColorlessMana(1),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "Sacrifice this token: Add {C}.",
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/9/b999a0fe-d2d0-4367-9abb-6ce5f3764f19.jpg?1783938121",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Winona Nelson"
        flavorText = "Ulamog's progeny swarm the land, draining Zendikar's mana to feed the titan's insatiable hunger."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d248ed0-c2b4-4558-9a09-ace8fd4b0599.jpg?1783938190"
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
