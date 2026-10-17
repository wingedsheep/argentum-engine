package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scuttling Butler
 * {3}
 * Artifact Creature — Construct
 * 4/1
 * At the beginning of combat on your turn, if you control two or more multicolored permanents,
 * this creature gains double strike until end of turn.
 *
 * Intervening-if (checked both when the trigger would fire and on resolution) over
 * [Conditions.YouControlAtLeast] counting multicolored permanents you control.
 */
val ScuttlingButler = card("Scuttling Butler") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Construct"
    oracleText = "At the beginning of combat on your turn, if you control two or more multicolored permanents, " +
        "this creature gains double strike until end of turn."
    power = 4
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControlAtLeast(2, GameObjectFilter.Permanent and GameObjectFilter.Multicolored)
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.Self)
        description = "At the beginning of combat on your turn, if you control two or more multicolored " +
            "permanents, this creature gains double strike until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Gaboleps"
        flavorText = "Porter, cleaner, and bouncer all in one."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0edbece0-8444-4634-a47b-7c88d1b8325e.jpg?1783923059"
    }
}
