package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Sergeant-at-Arms
 * {2}{W}
 * Creature — Human Soldier
 * 2/3
 * Kicker {2}{W}
 * When this creature enters, if it was kicked, create two 1/1 white Soldier creature tokens.
 */
val SergeantAtArms = card("Sergeant-at-Arms") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Kicker {2}{W}\nWhen this creature enters, if it was kicked, create two 1/1 white Soldier creature tokens."

    keywordAbility(KeywordAbility.kicker("{2}{W}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/c/c/cc7d137c-f6c0-44e5-af9f-a8bbd52d3b2a.jpg?1562702338"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Scott Murphy"
        flavorText = "\"Knights get the glory. Soldiers get things done.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f70b1ee-47b6-4904-850d-8ea8064bd27d.jpg?1562739444"
    }
}
