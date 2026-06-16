package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Citanul Druid
 * {1}{G}
 * Creature — Human Druid
 * 1/1
 * Whenever an opponent casts an artifact spell, put a +1/+1 counter on this creature.
 */
val CitanulDruid = card("Citanul Druid") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 1
    toughness = 1
    oracleText = "Whenever an opponent casts an artifact spell, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.opponentCasts(GameObjectFilter.Artifact)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Jeff A. Menges"
        flavorText = "Driven mad by the fall of Argoth, the Citanul Druids found peace only in battle."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8a130dc-3b1f-4fae-8459-b26bb5647fec.jpg?1562947598"
    }
}
