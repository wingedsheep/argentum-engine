package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Risona, Asari Commander — Kamigawa: Neon Dynasty #233 (canonical printing)
 * {1}{R}{W} · Legendary Creature — Human Samurai · 3/3
 *
 * Haste
 * Whenever Risona deals combat damage to a player, if it doesn't have an indestructible counter on
 * it, put an indestructible counter on it.
 * Whenever combat damage is dealt to you, remove an indestructible counter from Risona.
 *
 * "Combat damage is dealt to you" is the defensive batch trigger: once per combat-damage step
 * however many creatures connected. Only creatures deal combat damage, so the creature filter is
 * the printed "combat damage" — and an attacker that died to the same damage (a trampler that
 * traded with its blocker) still counts, read from its last-known information.
 */
val RisonaAsariCommander = card("Risona, Asari Commander") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Samurai"
    power = 3
    toughness = 3
    oracleText = "Haste\nWhenever Risona deals combat damage to a player, if it doesn't have an " +
        "indestructible counter on it, put an indestructible counter on it.\nWhenever combat damage " +
        "is dealt to you, remove an indestructible counter from Risona."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.Not(Conditions.SourceHasCounter(CounterType.INDESTRUCTIBLE))
        effect = Effects.AddCounters(CounterType.INDESTRUCTIBLE, 1, EffectTarget.Self)
        description = "Whenever Risona deals combat damage to a player, if it doesn't have an " +
            "indestructible counter on it, put an indestructible counter on it."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToYou()
        effect = Effects.RemoveCounters(CounterType.INDESTRUCTIBLE, 1, EffectTarget.Self)
        description = "Whenever combat damage is dealt to you, remove an indestructible counter from Risona."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51d8f1fa-ca92-4e2e-91e8-195bdad82def.jpg?1783923831"
        ruling(
            "2022-02-18",
            "Removing the indestructible counter from Risona may cause it to die if it has taken lethal " +
                "damage, usually because it blocked in the same combat that you took damage."
        )
    }
}
