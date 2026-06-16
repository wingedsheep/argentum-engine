package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Samwise the Stouthearted
 * {1}{W}
 * Legendary Creature — Halfling Peasant
 * 2/1
 * Flash
 *
 * When Samwise enters, choose up to one target permanent card in your graveyard that was
 * put there from the battlefield this turn. Return it to your hand. Then the Ring tempts you.
 *
 * Composition:
 *  - `Flash` keyword.
 *  - ETB trigger with an optional target restricted to "permanent card in your
 *    graveyard put there from battlefield this turn" — uses the new
 *    `StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn` (LTR Gap 20) on a
 *    `Permanent.ownedByYou()` filter in `Zone.GRAVEYARD`.
 *  - `Move(target, Zone.HAND, fromZone = Zone.GRAVEYARD)` for the return, followed by
 *    `Effects.TheRingTemptsYou()` regardless of whether a target was chosen.
 */
val SamwiseTheStouthearted = card("Samwise the Stouthearted") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Halfling Peasant"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "When Samwise enters, choose up to one target permanent card in your graveyard " +
        "that was put there from the battlefield this turn. Return it to your hand. " +
        "Then the Ring tempts you."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val returnTarget = target(
            "permanent card in your graveyard that was put there from the battlefield this turn",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Permanent
                        .ownedByYou()
                        .putIntoGraveyardFromBattlefieldThisTurn(),
                    zone = Zone.GRAVEYARD
                ),
                optional = true
            )
        )
        effect = Effects.Move(returnTarget, Zone.HAND, fromZone = Zone.GRAVEYARD)
            .then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        flavorText = "\"You've hurt my master, you brute, and you'll pay for it.\""
        artist = "Irvin Rodriguez"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/214c270e-29ca-4d69-bea6-9252ae7707ad.jpg?1686967902"
    }
}
