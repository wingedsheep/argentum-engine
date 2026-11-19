package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mortal's Ardor
 * {W}
 * Instant
 *
 * Target creature gets +1/+1 and gains lifelink until end of turn. (Damage dealt by the creature
 * also causes its controller to gain that much life.)
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val MortalsArdor = card("Mortal's Ardor") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+1 and gains lifelink until end of turn. " +
        "(Damage dealt by the creature also causes its controller to gain that much life.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, creature) then Effects.GrantKeyword(Keyword.LIFELINK, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Kev Walker"
        flavorText = "\"Such deeds should be common fare for such as us.\"\n—Anax, king of Akros"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e29f95c-aeed-4cb6-a43b-2748a5dd6138.jpg?1783939578"
    }
}
