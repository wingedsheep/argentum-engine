package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Marauding Maulhorn — Magic 2014 #145
 * {2}{R}{R} · Creature — Beast · 5/3
 * This creature attacks each combat if able unless you control a creature named Advocate of the Beast.
 *
 * [MustAttack] gated by a [ConditionalStaticAbility] whose condition is "you control no creature
 * named Advocate of the Beast" — [Conditions.YouControl] with `negate = true` (Reckless Cohort's
 * shape). The gate is evaluated during projection, so it is read as attackers are declared.
 */
val MaraudingMaulhorn = card("Marauding Maulhorn") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 3
    oracleText = "This creature attacks each combat if able unless you control a creature named Advocate of the Beast."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = MustAttack(),
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.named("Advocate of the Beast"),
                negate = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Jesper Ejsing"
        flavorText = "\"It's not just you. Everyone makes it angry.\"\n—Ardenoth, elvish nature advocate"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7d5e3dc-f307-4f91-a5ee-e7c5d03d8102.jpg?1783939912"
        ruling(
            "2013-07-01",
            "Marauding Maulhorn can attack if you control a creature named Advocate of the Beast. It just isn't forced to."
        )
        ruling(
            "2013-07-01",
            "Marauding Maulhorn checks whether you control a creature named Advocate of the Beast as attacking " +
                "creatures are declared. At that time, if you don't, Marauding Maulhorn attacks if able."
        )
    }
}
