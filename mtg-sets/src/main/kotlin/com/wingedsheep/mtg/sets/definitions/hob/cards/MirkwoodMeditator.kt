package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirkwood Meditator
 * {2}{U}
 * Creature — Elf Druid
 * 2/4
 * Landfall — Whenever a land you control enters, you may have this creature's base power and
 * toughness become 4/2 until end of turn.
 *
 * A base-stat *swap*, not a pump: [Effects.SetBasePowerAndToughness] writes Layer 7b set values, so
 * the 2/4 body becomes 4/2 and any +1/+1 counters or Layer 7c modifiers still apply on top. The
 * choice is genuinely the controller's each time a land lands, hence [MayEffect] rather than an
 * unconditional effect — with the printed body already 2/4, taking the 4/2 is often the wrong call.
 */
val MirkwoodMeditator = card("Mirkwood Meditator") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elf Druid"
    oracleText = "Landfall — Whenever a land you control enters, you may have this creature's base " +
        "power and toughness become 4/2 until end of turn."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = MayEffect(
            Effects.SetBasePowerAndToughness(power = 4, toughness = 2, target = EffectTarget.Self),
            descriptionOverride = "Have Mirkwood Meditator's base power and toughness become 4/2 until end of turn?"
        )
        description = "Landfall — Whenever a land you control enters, you may have this creature's " +
            "base power and toughness become 4/2 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Francisco Miyara"
        flavorText = "Elves always knew what was going on among the peoples of the land—as quick as water flows, or quicker."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad7ed4e6-3fe2-40f1-909b-a03b2a3c941a.jpg?1785497064"
    }
}
