package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Slip On the Ring
 * {1}{W}
 * Instant
 *
 * Exile target creature you own, then return it to the battlefield under your control.
 * The Ring tempts you.
 *
 * Composes the flicker out of exile → return (the established pattern, see Meneldor, Swift Savior):
 * the target is restricted to a creature you own, so returning it to the battlefield under its
 * owner's control IS returning it under your control. Followed by the Ring-tempts rider.
 */
val SlipOnTheRing = card("Slip On the Ring") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature you own, then return it to the battlefield under your control. " +
        "The Ring tempts you."

    spell {
        val creature = target(
            "creature you own",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.ownedByYou()))
        )
        effect = Effects.Composite(
            listOf(
                Effects.Move(creature, Zone.EXILE),
                Effects.Move(creature, Zone.BATTLEFIELD),
                Effects.TheRingTemptsYou()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        flavorText = "How had it come to be on his finger? He wondered if the Ring itself had not played him a trick."
        artist = "Iga Oliwiak"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e20c2b8-ffe3-4d14-8588-f89719358e3d.jpg?1686967929"
    }
}
