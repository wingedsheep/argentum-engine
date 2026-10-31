package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Giant Opportunity — Throne of Eldraine #159
 * {2}{G} · Sorcery
 *
 * You may sacrifice two Foods. If you do, create a 7/7 green Giant creature token. Otherwise,
 * create three Food tokens.
 *
 * The sacrifice is a resolution-time choice, not a casting cost (2019-10-04 ruling), so it's a
 * [Effects.MayPay] gate over `SacrificeOwn(Food, count = 2)`: the gate only offers "yes" while
 * you control two Foods (a cost is paid in full or not at all), and declining — or being unable
 * to pay — runs the three-Food branch. The Food filter is subtype-based, so any Food artifact
 * (not just tokens) can be sacrificed.
 */
val GiantOpportunity = card("Giant Opportunity") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "You may sacrifice two Foods. If you do, create a 7/7 green Giant creature token. " +
        "Otherwise, create three Food tokens. (They're artifacts with \"{2}, {T}, Sacrifice this " +
        "token: You gain 3 life.\")"

    spell {
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(GameObjectFilter.Any.withSubtype("Food"), count = 2),
            then = Effects.CreateToken(
                power = 7,
                toughness = 7,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Giant"),
                imageUri = "https://cards.scryfall.io/normal/front/3/c/3c1debc4-0b12-4848-b551-90680ab8c0ab.jpg?1783932481",
            ),
            otherwise = Effects.CreateFood(3),
            descriptionOverride = "You may sacrifice two Foods. If you do, create a 7/7 green Giant " +
                "creature token. Otherwise, create three Food tokens.",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "James Paick"
        flavorText = "They look tasty, but best to plant them."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40383646-3fc4-4267-b9ad-bf90a85972fc.jpg?1783932609"

        ruling(
            "2019-10-04",
            "You choose whether to sacrifice two Foods as Giant Opportunity resolves, not as you cast it."
        )
        ruling(
            "2019-10-04",
            "You can't sacrifice more than two Foods to create more than one Giant token."
        )
        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token."
        )
    }
}
