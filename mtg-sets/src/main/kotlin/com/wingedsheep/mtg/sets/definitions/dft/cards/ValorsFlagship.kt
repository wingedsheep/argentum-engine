package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Valor's Flagship — Aetherdrift #35
 * {4}{W}{W}{W} · Legendary Artifact — Vehicle · 7/7
 *
 * Flying, first strike, lifelink
 * Crew 3
 * Cycling {X}{2}{W}
 * When you cycle this card, create X 1/1 colorless Pilot creature tokens with "This token saddles
 * Mounts and crews Vehicles as though its power were 2 greater."
 *
 * Cycling is an activated ability (CR 702.29a), so X is announced as it's activated (CR 107.3a).
 * The engine carries that announced X on `CardCycledEvent` into the trigger's context, where
 * [DynamicAmount.XValue] reads it — cycling for X=0 legally creates no tokens.
 */
val ValorsFlagship = card("Valor's Flagship") {
    manaCost = "{4}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Vehicle"
    power = 7
    toughness = 7
    oracleText = "Flying, first strike, lifelink\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This " +
        "Vehicle becomes an artifact creature until end of turn.)\n" +
        "Cycling {X}{2}{W} ({X}{2}{W}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, create X 1/1 colorless Pilot creature tokens with \"This token " +
        "saddles Mounts and crews Vehicles as though its power were 2 greater.\""

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    keywordAbility(KeywordAbility.crew(3))

    keywordAbility(KeywordAbility.cycling("{X}{2}{W}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = Effects.CreateToken(
            count = DynamicAmount.XValue,
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Pilot"),
            imageUri = "https://cards.scryfall.io/normal/front/8/6/8672d795-04f9-4089-9c92-6d6ff628da12.jpg?1783907682",
            staticAbilities = listOf(CrewSaddleContribution(modifier = 2))
        )
        description = "When you cycle this card, create X 1/1 colorless Pilot creature tokens " +
            "with \"This token saddles Mounts and crews Vehicles as though its power were 2 greater.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "35"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8af1dddf-6c95-448b-acc8-df5a99202e9a.jpg?1783907911"
    }
}
