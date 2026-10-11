package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thopter Engineer
 * {2}{R}
 * Creature — Human Artificer
 * 1/3
 * When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying.
 * Artifact creatures you control have haste.
 */
val ThopterEngineer = card("Thopter Engineer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying.\n" +
        "Artifact creatures you control have haste. (They can attack and {T} as soon as they come under your control.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5e6c48a-29d0-40ae-aac6-df7319873f50.jpg"
    }
}
