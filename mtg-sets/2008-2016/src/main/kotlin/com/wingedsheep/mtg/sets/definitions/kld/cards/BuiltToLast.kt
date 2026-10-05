package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Built to Last — Kaladesh #7
 * {W} · Instant
 *
 * Target creature gets +2/+2 until end of turn. If it's an artifact creature, it gains
 * indestructible until end of turn.
 *
 * The +2/+2 always applies; the indestructible grant is an additional, resolution-time rider
 * ([Effects.If] over [Conditions.TargetMatchesFilter] on the same target) — an artifact creature
 * gets both, not one instead of the other (the card's ruling).
 *
 * Canonical printing is Kaladesh (its earliest real printing); Jumpstart 2022 is a
 * [com.wingedsheep.sdk.model.Printing] row (`.../definitions/j22/cards/BuiltToLastReprint.kt`).
 */
val BuiltToLast = card("Built to Last") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If it's an artifact creature, it gains " +
        "indestructible until end of turn. (Damage and effects that say \"destroy\" don't destroy it.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.ArtifactCreature, creature),
                then = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Svetlin Velinov"
        flavorText = "Consulate-built automatons have a lifetime warranty."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef3e09a4-93d0-4ed7-bbee-82108672d5f8.jpg?1783937236"
        ruling("2016-09-20", "An artifact creature gains indestructible in addition to getting +2/+2, not instead of it.")
    }
}
