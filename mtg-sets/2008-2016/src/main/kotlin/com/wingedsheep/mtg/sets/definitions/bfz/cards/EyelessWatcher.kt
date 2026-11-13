package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Eyeless Watcher
 * {3}{G}
 * Creature — Eldrazi Drone
 * 1/1
 * Devoid (This card has no color.)
 * When this creature enters, create two 1/1 colorless Eldrazi Scion creature tokens. They have
 * "Sacrifice this token: Add {C}."
 *
 * The Scion token is the same inline shape as Brood Monitor's.
 */
val EyelessWatcher = card("Eyeless Watcher") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "When this creature enters, create two 1/1 colorless Eldrazi Scion creature tokens. " +
        "They have \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.enters()
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
        collectorNumber = "166"
        artist = "Yohann Schepacz"
        flavorText = "Every Eldrazi in Ulamog's lineage is an extension of the titan's will."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d949d0e-baf7-4573-bb15-7e30e3e9b202.jpg?1783938189"
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
