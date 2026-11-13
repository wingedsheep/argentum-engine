package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Catacomb Sifter
 * {1}{B}{G}
 * Creature — Eldrazi Drone
 * 2/3
 * Devoid (This card has no color.)
 * When this creature enters, create a 1/1 colorless Eldrazi Scion creature token. It has
 * "Sacrifice this token: Add {C}."
 * Whenever another creature you control dies, scry 1.
 *
 * The Scion token is the same inline shape as Brood Butcher's and Incubator Drone's.
 */
val CatacombSifter = card("Catacomb Sifter") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "When this creature enters, create a 1/1 colorless Eldrazi Scion creature token. " +
        "It has \"Sacrifice this token: Add {C}.\"\n" +
        "Whenever another creature you control dies, scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)"

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

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.Scry(1)
        description = "Whenever another creature you control dies, scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c3b220b-e699-45b0-8ec1-1022c1f50329.jpg?1783938182"
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
