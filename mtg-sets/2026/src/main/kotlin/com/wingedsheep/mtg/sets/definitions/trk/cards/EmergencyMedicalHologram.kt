package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Emergency Medical Hologram
 * {W}
 * Artifact Creature — Illusion Doctor
 * 0/1
 *
 * When this creature enters, put a +1/+1 counter on target creature. You gain 1 life.
 */
val EmergencyMedicalHologram = card("Emergency Medical Hologram") {
    manaCost = "{W}"
    typeLine = "Artifact Creature — Illusion Doctor"
    colorIdentity = "W"
    oracleText = "When this creature enters, put a +1/+1 counter on target creature. You gain 1 life."
    power = 0
    toughness = 1
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then Effects.GainLife(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Kirk DouPonce"
        flavorText = "The EMH Mark 1 distills thousands of medical references and the experiences of forty-seven medical officers into a single unit, though its bedside manner leaves much to be desired."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dda6f965-cbdf-4dbc-b0b1-00c27e32f5f2.jpg?1786005719"
    }
}

