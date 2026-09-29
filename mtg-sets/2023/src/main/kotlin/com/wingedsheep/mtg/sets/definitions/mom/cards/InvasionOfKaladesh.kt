package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invasion of Kaladesh // Aetherwing, Golden-Scale Flagship — March of the Machine #234.
 * {U}{R} · Battle — Siege · defense 4 // Legendary Artifact — Vehicle, power CDA / toughness 4
 *
 * Front: the Siege's enter trigger makes a 1/1 colorless flying Thopter artifact creature token.
 * Back: flying, crew 1, and a power CDA counting the artifacts you control — Aetherwing counts
 * itself while it's still an artifact (per the ruling).
 */
private val InvasionOfKaladeshFront = card("Invasion of Kaladesh") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, create a 1/1 colorless Thopter artifact creature token with flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            name = "Thopter",
            imageUri = "https://cards.scryfall.io/normal/front/d/b/db0041d8-cacd-4057-8f99-37810edb4b7e.jpg?1783916667",
        )
        description = "When this Siege enters, create a 1/1 colorless Thopter artifact creature " +
            "token with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f7231f6-8adc-4a68-9984-e56b974c087b.jpg?1783916951"
    }
}

private val AetherwingGoldenScaleFlagship = card("Aetherwing, Golden-Scale Flagship") {
    manaCost = ""
    colorIdentity = "UR"
    colorIndicator = "UR"
    typeLine = "Legendary Artifact — Vehicle"
    toughness = 4
    oracleText = "Flying\n" +
        "Aetherwing's power is equal to the number of artifacts you control.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)
    dynamicPower(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count())
    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Leon Tukker"
        flavorText = "Inspired by the grace of Ixalan's pterodons, Saheeli built one of her own."
        imageUri = "https://cards.scryfall.io/normal/back/4/f/4f7231f6-8adc-4a68-9984-e56b974c087b.jpg?1783916951"
        ruling("2023-04-14", "As long as Aetherwing is still an artifact, its ability will count Aetherwing itself.")
    }
}

val InvasionOfKaladesh: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfKaladeshFront,
    backFace = AetherwingGoldenScaleFlagship,
)
