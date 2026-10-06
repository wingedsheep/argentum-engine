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
 * Blisterpod
 * {G}
 * Creature — Eldrazi Drone
 * 1/1
 * Devoid (This card has no color.)
 * When this creature dies, create a 1/1 colorless Eldrazi Scion creature token. It has
 * "Sacrifice this token: Add {C}."
 */
val Blisterpod = card("Blisterpod") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "When this creature dies, create a 1/1 colorless Eldrazi Scion creature token. " +
        "It has \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.dies()
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
            imageUri = "https://cards.scryfall.io/normal/front/a/b/abcdf03d-d237-4ceb-91fa-8f95341af369.jpg?1783938121",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Ryan Barger"
        flavorText = "A blisterpod's corpse is a scion's cradle."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e16f1803-634a-41b0-ae21-484d6f914a0d.jpg?1783938190"
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
