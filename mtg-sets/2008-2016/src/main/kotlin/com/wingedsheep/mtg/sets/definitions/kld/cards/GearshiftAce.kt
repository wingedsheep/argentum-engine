package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Gearshift Ace — Kaladesh #16. */
val GearshiftAce = card("Gearshift Ace") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Pilot"
    power = 2
    toughness = 1
    oracleText = "First strike\nWhenever this creature crews a Vehicle, that Vehicle gains first strike until end of turn."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.crews()
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Winona Nelson"
        flavorText = "Some display their trophies at the Hall of Mechanists. Others display their talent in the streets of Ghirapur."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e8c6a9d-6169-40f8-8b3e-53ebf75be663.jpg?1783937232"
        ruling("2016-09-20", "Gearshift Ace's triggered ability triggers when it becomes tapped to activate a crew ability. Even if Gearshift Ace leaves the battlefield before its triggered ability resolves, that Vehicle still gains first strike until end of turn.")
    }
}
