package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Valor Singer
 * {2}{R}
 * Creature — Tiefling Bard
 * 2/3
 * Combat Inspiration — At the beginning of combat on your turn, target creature you control gets
 * +1/+0 until end of turn.
 *
 * "Combat Inspiration" is an ability word with no rules meaning; the trigger is the same shape as
 * Blood Mist's (`Triggers.you.beginningOf(Step.BEGIN_COMBAT)` with a creature-you-control target).
 */
val ValorSinger = card("Valor Singer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Tiefling Bard"
    oracleText = "Combat Inspiration — At the beginning of combat on your turn, target creature you " +
        "control gets +1/+0 until end of turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.ModifyStats(1, 0, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Justyna Dura"
        flavorText = "\"There is only one way forward: into legend.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89bc162c-bdf1-43f7-882f-d8cee4f3f415.jpg?1783926470"
    }
}
