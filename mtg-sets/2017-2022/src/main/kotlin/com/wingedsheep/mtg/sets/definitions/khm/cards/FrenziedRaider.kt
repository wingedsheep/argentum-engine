package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OnceOnlyAbilityKind
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frenzied Raider — Kaldheim #137 (canonical printing)
 * {1}{R} · Creature — Demon Berserker · 2/2
 *
 * Whenever you activate a boast ability, put a +1/+1 counter on this creature.
 *
 * The activation event fires as the boast ability is put on the stack, so the counter lands
 * before the boast resolves (per the ruling).
 */
val FrenziedRaider = card("Frenzied Raider") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Demon Berserker"
    power = 2
    toughness = 2
    oracleText = "Whenever you activate a boast ability, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.activatesAbility(onceOnlyKind = OnceOnlyAbilityKind.BOAST)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Lius Lasahido"
        flavorText = "\"Arrogant maggot. You won't be so proud when I rip out your tongue and drink from your skull!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22c59e0b-5b78-4cf5-9c2a-92ccf833778f.jpg?1783928228"
        ruling("2021-02-05", "Frenzied Raider's triggered ability will resolve before the boast ability that caused it to trigger.")
    }
}
