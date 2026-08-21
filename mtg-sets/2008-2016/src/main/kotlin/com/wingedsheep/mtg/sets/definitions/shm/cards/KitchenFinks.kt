package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kitchen Finks
 * {1}{G/W}{G/W}
 * Creature — Ouphe
 * 3 / 2
 *
 * When this creature enters, you gain 2 life.
 * Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield
 * under its owner's control with a -1/-1 counter on it.)
 *
 * - Persist is engine-live: [Keyword.PERSIST] is read by the death-trigger detector, so the keyword
 *   alone carries the return. The ETB gain-life rides back with the body, which is the card's point.
 * - The ETB is a plain untargeted [Triggers.EntersBattlefield] + [Effects.GainLife], matching Assay's
 *   `ZoneChangeEvent -> Battlefield` / `GainLife(Fixed 2)` reading.
 */
val KitchenFinks = card("Kitchen Finks") {
    manaCost = "{1}{G/W}{G/W}"
    typeLine = "Creature — Ouphe"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you gain 2 life.\n" +
        "Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the " +
        "battlefield under its owner's control with a -1/-1 counter on it.)"

    keywords(Keyword.PERSIST)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(2)
        description = "When this creature enters, you gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Kev Walker"
        flavorText = "Accept one favor from an ouphe, and you're doomed to accept another."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d203208-8a21-4c68-afa2-4efd8726f026.jpg?1783942717"
    }
}
