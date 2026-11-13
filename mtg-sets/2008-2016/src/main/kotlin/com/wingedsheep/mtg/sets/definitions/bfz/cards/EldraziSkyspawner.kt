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
 * Eldrazi Skyspawner
 * {2}{U}
 * Creature — Eldrazi Drone
 * 2/1
 * Devoid (This card has no color.)
 * Flying
 * When this creature enters, create a 1/1 colorless Eldrazi Scion creature token. It has
 * "Sacrifice this token: Add {C}."
 *
 * The Scion token is the same inline shape as Eyeless Watcher's.
 */
val EldraziSkyspawner = card("Eldrazi Skyspawner") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "When this creature enters, create a 1/1 colorless Eldrazi Scion creature token. " +
        "It has \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID, Keyword.FLYING)

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
        collectorNumber = "58"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c9c1a10-446e-492a-95cc-a459dc6c08a0.jpg?1783938213"
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
