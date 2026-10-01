package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gitaxian Anatomist
 * {3}{U}
 * Creature — Phyrexian Wizard
 * 2/5
 *
 * When this creature enters, you may tap it. If you do, proliferate.
 *
 * "You may tap it. If you do" — tapping an already-tapped permanent isn't tapping it, so the
 * offer is only made while this creature is still untapped (and still on the battlefield) at
 * resolution; otherwise nothing happens.
 */
val GitaxianAnatomist = card("Gitaxian Anatomist") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Phyrexian Wizard"
    power = 2
    toughness = 5
    oracleText = "When this creature enters, you may tap it. If you do, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.SourceIsUntapped,
            then = Effects.May(
                effect = Effects.Tap(EffectTarget.Self) then Effects.Proliferate(),
                sourceRequiredZone = Zone.BATTLEFIELD,
            ),
            descriptionOverride = "You may tap this creature. If you do, proliferate."
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Sam Wolfe Connelly"
        flavorText = "An inconsistency in a routine vivisection sparked a revelation about the inner workings of the Orthodoxy's centurions."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63517062-77da-4b60-aa24-f91553a81bed.jpg?1783918064"
    }
}
