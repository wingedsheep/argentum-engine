package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clockspinning — Time Spiral #53 (canonical printing)
 * {U} · Instant
 *
 * Buyback {3}
 * Choose a counter on target permanent or suspended card. Remove that counter from that permanent
 * or card or put another of those counters on it.
 *
 * "Suspended card" is the CR 702.62b definition (`suspended()`, in exile), unioned with any
 * permanent as one cross-zone target. Whether to add or remove and which kind are both chosen on
 * resolution (the rulings); picking the action first and then the kind reaches exactly the same
 * outcomes. Taking a suspended card's last time counter fires suspend's cast trigger right away.
 */
val Clockspinning = card("Clockspinning") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Buyback {3} (You may pay an additional {3} as you cast this spell. If you do, put this card into your hand as it resolves.)\n" +
        "Choose a counter on target permanent or suspended card. Remove that counter from that permanent or card or put another of those counters on it."

    keywordAbility(KeywordAbility.buyback("{3}"))

    spell {
        val t = target(
            TargetFilter.Permanent.or(TargetFilter(GameObjectFilter.Any.suspended()).inZone(Zone.EXILE))
        )
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(label = "Remove a counter from it", effect = Effects.RemoveCounterOfAnyKind(t)),
                EffectChoice(label = "Put another of those counters on it", effect = Effects.AddCountersOfChosenKind(t)),
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Zoltan Boros & Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1323d548-e2fe-47c5-8df3-f181aed537c5.jpg?1783943246"
    }
}
