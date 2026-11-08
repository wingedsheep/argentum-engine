package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Speedway Fanatic — Kaladesh #132. */
val SpeedwayFanatic = card("Speedway Fanatic") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pilot"
    power = 2
    toughness = 1
    oracleText = "Haste\nWhenever this creature crews a Vehicle, that Vehicle gains haste until end of turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.crews()
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Slawomir Maniak"
        flavorText = "If her vehicle even has a brake, she couldn't tell you which pedal it is."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/436ce901-6ac6-4f8c-8ff0-18103f2642b8.jpg?1783937188"
        ruling("2016-09-20", "Speedway Fanatic's triggered ability triggers when it becomes tapped to activate a crew ability. Even if Speedway Fanatic leaves the battlefield before its triggered ability resolves, that Vehicle still gains haste until end of turn.")
    }
}
