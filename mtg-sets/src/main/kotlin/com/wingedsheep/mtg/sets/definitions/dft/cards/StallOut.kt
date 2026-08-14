package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Stall Out — Aetherdrift #66
 * {1}{U} · Sorcery
 *
 * Tap target creature or Vehicle, then put three stun counters on it.
 * Cycling {2}
 *
 * Tap and counters are one composite on a single target, in printed order: an already-tapped
 * creature still receives all three stun counters, and the counters land on an uncrewed Vehicle
 * just as well (a Vehicle matches [GameObjectFilter.CreatureOrVehicle] by subtype whether or not it
 * is currently a creature — and tapping it is what the card is for).
 */
val StallOut = card("Stall Out") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap target creature or Vehicle, then put three stun counters on it. (If a " +
        "permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        val t = target(
            "target creature or Vehicle",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))
        )
        effect = Effects.Composite(
            Effects.Tap(t),
            Effects.AddCounters(Counters.STUN, 3, t)
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Inkognit"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ea0e0d3-833f-4353-b648-57b0b657cc1c.jpg?1783907902"
    }
}
