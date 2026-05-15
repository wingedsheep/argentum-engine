package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan

val ProfessionalWrestler = card("Professional Wrestler") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior Performer"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\nThis creature can't be blocked by more than one creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTreasure(1)
    }

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Kevin Sidharta"
        flavorText = "\"Any of you! One-on-one! No one can stay in the ring for three minutes with me!\"\n—Crusher Hogan"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a5381e7-ddda-47e7-886d-812250ffb745.jpg?1757377496"
    }
}
