package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Marauding Dreadship — March of the Machine #153.
 * {2}{R} · Artifact — Vehicle 4/1
 */
val MaraudingDreadship = card("Marauding Dreadship") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 1
    oracleText = "Haste\nWhen this Vehicle enters, incubate 2. (Create an Incubator token with two +1/+1 counters on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)\nCrew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(2)
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Tiffany Turrill"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15625d6a-3844-4d23-ab35-ee3c3508db8d.jpg?1783916986"
    }
}
