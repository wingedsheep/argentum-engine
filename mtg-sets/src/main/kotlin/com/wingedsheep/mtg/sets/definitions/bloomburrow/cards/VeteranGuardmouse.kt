package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Veteran Guardmouse {3}{R/W}
 * Creature — Mouse Soldier
 * 3/4
 *
 * Valiant — Whenever this creature becomes the target of a spell or ability
 * you control for the first time each turn, it gets +1/+0 and gains first
 * strike until end of turn. Scry 1.
 */
val VeteranGuardmouse = card("Veteran Guardmouse") {
    manaCost = "{3}{R/W}"
    typeLine = "Creature — Mouse Soldier"
    power = 3
    toughness = 4
    oracleText = "Valiant — Whenever this creature becomes the target of a spell or ability you control for the first time each turn, it gets +1/+0 and gains first strike until end of turn. Scry 1."

    triggeredAbility {
        trigger = Triggers.Valiant
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
            .then(Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self))
            .then(EffectPatterns.scry(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "237"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3db43c46-b616-4ef8-80ed-0fab345ab3d0.jpg?1721427216"
        ruling("2024-07-26", "If Veteran Guardmouse leaves the battlefield before its ability resolves, you'll still scry 1.")
    }
}
