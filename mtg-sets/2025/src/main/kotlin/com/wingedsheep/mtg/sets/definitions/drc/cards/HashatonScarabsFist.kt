package com.wingedsheep.mtg.sets.definitions.drc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hashaton, Scarab's Fist — Aetherdrift Commander #1 (canonical printing)
 * {W}{B} · Legendary Creature — Zombie Wizard · 1/3
 *
 * Whenever you discard a creature card, you may pay {2}{U}. If you do, create a tapped token
 * that's a copy of that card, except it's a 4/4 black Zombie.
 *
 * `Triggers.you.discards(Creature)` fires once per discarded creature card and binds it as the
 * triggering entity; the copy reads that card's printed characteristics, with the 4/4, black and
 * Zombie exceptions overriding its base P/T, colors and creature types (copiable values).
 */
val HashatonScarabsFist = card("Hashaton, Scarab's Fist") {
    manaCost = "{W}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Zombie Wizard"
    power = 1
    toughness = 3
    oracleText = "Whenever you discard a creature card, you may pay {2}{U}. If you do, create a tapped " +
        "token that's a copy of that card, except it's a 4/4 black Zombie."

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Creature)
        effect = Effects.MayPay(
            ManaCost.parse("{2}{U}"),
            Effects.CreateTokenCopyOfTarget(
                EffectTarget.TriggeringEntity,
                tapped = true,
                overridePower = 4,
                overrideToughness = 4,
                overrideColors = setOf(Color.BLACK),
                overrideSubtypes = setOf(Subtype.ZOMBIE)
            )
        )
        description = "Whenever you discard a creature card, you may pay {2}{U}. If you do, create a tapped " +
            "token that's a copy of that card, except it's a 4/4 black Zombie."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02645651-cd55-4bd0-8a4d-fa257270a0e0.jpg?1783907754"
    }
}
