package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Mister Negative — Marvel's Spider-Man #135
 * {5}{W}{B} · Legendary Creature — Human Villain · 5/5
 *
 * Vigilance, lifelink
 * Darkforce Inversion — When Mister Negative enters, you may exchange life totals with target
 * opponent. If you lost life this way, draw that many cards.
 *
 * The exchange uses the new `Effects.ExchangeLifeTotals(drawEqualToLifeLost = true)` (CR 701.12c).
 */
val MisterNegative = card("Mister Negative") {
    manaCost = "{5}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Villain"
    power = 5
    toughness = 5
    oracleText = "Vigilance, lifelink\n" +
        "Darkforce Inversion — When Mister Negative enters, you may exchange life totals with " +
        "target opponent. If you lost life this way, draw that many cards."

    keywords(Keyword.VIGILANCE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", Targets.Opponent)
        effect = MayEffect(
            effect = Effects.ExchangeLifeTotals(target = opponent, drawEqualToLifeLost = true)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "135"
        artist = "Thanh Tuấn"
        flavorText = "\"Light and dark. They are in opposition, but both are necessary. I am both, and so I am all.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c9cb13d-55ff-4e26-aa49-755f8bcebc11.jpg?1783905315"
    }
}
