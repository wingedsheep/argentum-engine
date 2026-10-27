package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scorch Rider — Zendikar Rising #158
 * {3}{R} · Creature — Human Warrior · 4/3
 *
 * Kicker {1}{R}
 * When this creature enters, if it was kicked, it gains haste until end of turn.
 */
val ScorchRider = card("Scorch Rider") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "Kicker {1}{R} (You may pay an additional {1}{R} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, it gains haste until end of turn."
    power = 4
    toughness = 3

    keywordAbility(KeywordAbility.kicker("{1}{R}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"Lava flows where it pleases. I'm just happy to be along for the ride!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ec9d3eb-4729-49a9-8146-7388ff2629df.jpg"
    }
}
