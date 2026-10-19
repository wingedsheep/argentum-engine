package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Iron Apprentice
 * {1}
 * Artifact Creature — Construct
 * 0/0
 * This creature enters with a +1/+1 counter on it.
 * When this creature dies, if it had counters on it, put those counters on target creature you control.
 */
val IronApprentice = card("Iron Apprentice") {
    manaCost = "{1}"
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 0
    oracleText = "This creature enters with a +1/+1 counter on it.\n" +
        "When this creature dies, if it had counters on it, put those counters on target creature you control."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true
    ))

    triggeredAbility {
        val creatureYouControl = target(TargetFilter.CreatureYouControl)
        trigger = Triggers.self.dies()
        interveningIf = Conditions.TriggeringEntityHadCounters
        effect = Effects.MoveAllLastKnownCounters(creatureYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "248"
        artist = "Kekai Kotaki"
        flavorText = "\"I needed an extra set of hands, so I built one.\"\n—Kenji, Sokenzan artisan"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13d6d9fc-509b-42db-8ac1-85066eb6e9c4.jpg?1783923825"
    }
}
