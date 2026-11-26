package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Midvast Protector — Avacyn Restored #28
 * {3}{W} · Creature — Human Wizard · 2/3
 *
 * When this creature enters, target creature you control gains protection from the color of your
 * choice until end of turn.
 *
 * The color is chosen on resolution (Mother of Runes' shape).
 */
val MidvastProtector = card("Midvast Protector") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, target creature you control gains protection from the color " +
        "of your choice until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "James Ryman"
        flavorText = "\"I cultivated my faith in Avacyn despite meager returns. Now my faith is repaid.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4f6214f-90cb-4575-b221-3c8d0ed65ffe.jpg?1783940732"
    }
}
