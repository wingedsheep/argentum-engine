package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan

/**
 * Professional Wrestler
 * {3}{G}
 * Creature — Human Warrior Performer
 * 4/4
 * When this creature enters, create a Treasure token.
 * Professional Wrestler can't be blocked by more than one creature.
 */
val ProfessionalWrestler = card("Professional Wrestler") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior Performer"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, create a Treasure token.\nProfessional Wrestler can't be blocked by more than one creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTreasure(1)
    }

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f4c85b1-8c5b-4c6e-8e4e-d4b2a4e5f7a9.jpg?1757377869"
    }
}
