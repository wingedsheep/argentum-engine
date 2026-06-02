package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Camel
 * {W}
 * Creature — Camel
 * 0/1
 *
 * Banding (CR 702.22 — engine-supported keyword).
 * As long as this creature is attacking, prevent all damage Deserts would deal to this
 * creature and to creatures banded with this creature.
 *
 * The prevention is a continuous [PreventDamage] replacement effect (CR 615):
 *  - source filter = any Desert (a permanent with the Desert land subtype),
 *  - recipient filter = [GameObjectFilter.inSameBandAsSource], which matches Camel itself and
 *    any creature sharing Camel's combat band (CR 702.22). That filter only matches while Camel
 *    is attacking — band membership exists only during combat — so it encodes the "as long as
 *    this creature is attacking" clause without a separate condition.
 */
val Camel = card("Camel") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Camel"
    power = 0
    toughness = 1
    oracleText = "Banding (Any creatures with banding, and up to one without, can attack in a " +
        "band. Bands are blocked as a group. If any creatures with banding you control are " +
        "blocking or being blocked by a creature, you divide that creature's combat damage, " +
        "not its controller, among any of the creatures it's being blocked by or is blocking.)\n" +
        "As long as this creature is attacking, prevent all damage Deserts would deal to this " +
        "creature and to creatures banded with this creature."

    keywords(Keyword.BANDING)

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.Matching(GameObjectFilter.Any.inSameBandAsSource()),
                source = SourceFilter.Matching(GameObjectFilter.Land.withSubtype("Desert"))
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Sandra Everingham"
        flavorText = "Everyone knew Walid was a pious man, for he had been blessed with many sons, many jewels, and a great many Camels."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0078aa8-bfb8-43b0-a6b7-1991596c21e1.jpg?1562936810"
    }
}
