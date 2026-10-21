package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rebuild the City
 * {3}{B}{R}{G}
 * Sorcery
 *
 * Choose target land. Create three tokens that are copies of it, except they're 3/3 creatures in
 * addition to their other types and they have vigilance and menace. (They're affected by summoning
 * sickness.)
 *
 * The "except" rider maps onto [Effects.CreateTokenCopyOfTarget]'s copy exceptions (same shape as
 * Applied Geometry): `overridePower/Toughness = 3`, `addCardTypes = {CREATURE}` unioned on top of
 * the copied land types, and `addedKeywords = {VIGILANCE, MENACE}`. The copy reads the land's
 * copiable values, so counters, tapped state and non-copy type changes are not copied, while
 * "enters tapped" and ETB abilities of the copied land still apply (per the rulings).
 */
val RebuildTheCity = card("Rebuild the City") {
    manaCost = "{3}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Sorcery"
    oracleText = "Choose target land. Create three tokens that are copies of it, except they're " +
        "3/3 creatures in addition to their other types and they have vigilance and menace. " +
        "(They're affected by summoning sickness.)"

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.CreateTokenCopyOfTarget(
            target = land,
            count = 3,
            overridePower = 3,
            overrideToughness = 3,
            addCardTypes = setOf("CREATURE"),
            addedKeywords = setOf(Keyword.VIGILANCE, Keyword.MENACE),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Sergey Glushakov"
        flavorText = "The Phyrexian design had to go, but the materials could be put to good use."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7651eb7-3cbe-43de-8800-085be19e7f77.jpg?1783916509"
        ruling("2023-05-12", "Each of the tokens copy exactly what was printed on the original land and nothing else (unless that land is copying something else or is a token). It doesn't copy whether that land is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its types, color, and so on. However, if the original land has an ability that says it enters the battlefield tapped, the copies will also enter tapped.")
        ruling("2023-05-12", "Any enters-the-battlefield abilities of the copied land will trigger when the token enters the battlefield. Any \"as [this land] enters the battlefield\" or \"[this land] enters the battlefield with\" abilities of the copied land will also work.")
    }
}
