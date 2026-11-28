package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rakish Heir
 * {2}{R}
 * Creature — Vampire
 * 2/2
 * Whenever a Vampire you control deals combat damage to a player, put a +1/+1 counter on it.
 */
val RakishHeir = card("Rakish Heir") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    oracleText = "Whenever a Vampire you control deals combat damage to a player, put a +1/+1 counter on it."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Winona Nelson"
        flavorText = "\"If you're not having fun, what's the point of living forever?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4afab3a6-95e3-4786-94f2-d9aa7365a4de.jpg"
    }
}
