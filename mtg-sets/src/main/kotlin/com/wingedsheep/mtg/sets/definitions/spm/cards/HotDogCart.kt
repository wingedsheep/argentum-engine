package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

val HotDogCart = card("Hot Dog Cart") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "{T}: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
        description = "When this artifact enters, create a Food token."
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "David Álvarez"
        flavorText = "\"One with everything, please.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ee3b883-e9f5-426f-a2ea-96fe9ff3aba9.jpg?1757378016"
    }
}
