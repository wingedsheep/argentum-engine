package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule

val ScionSummoner = card("Scion Summoner") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "When this creature enters, create a 1/1 colorless Eldrazi Scion creature token. " +
        "It has \"Sacrifice this token: Add {C}.\" ({C} represents colorless mana.)"

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
            imageUri = "https://cards.scryfall.io/normal/front/a/0/a03edf14-3495-4f61-ad73-f16e9456472c.jpg?1783937863",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/826a882e-c4bd-4132-b797-9e1aa2d0bce4.jpg?1783937903"
    }
}
