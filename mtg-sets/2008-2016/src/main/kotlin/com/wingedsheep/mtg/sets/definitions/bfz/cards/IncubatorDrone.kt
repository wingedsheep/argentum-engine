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
 * Incubator Drone
 * {3}{U}
 * Creature — Eldrazi Drone
 * 2/3
 * Devoid (This card has no color.)
 * When this creature enters, create a 1/1 colorless Eldrazi Scion creature token. It has
 * "Sacrifice this token: Add {C}."
 *
 * The Scion token is the same inline shape as Eyeless Watcher's.
 */
val IncubatorDrone = card("Incubator Drone") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "When this creature enters, create a 1/1 colorless Eldrazi Scion creature token. " +
        "It has \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
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
        collectorNumber = "60"
        artist = "Cynthia Sheppard"
        flavorText = "The touch of an Eldrazi brings life and death in turn."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0855762-7068-4111-a6bf-fe6d5b092c74.jpg?1783938212"
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
