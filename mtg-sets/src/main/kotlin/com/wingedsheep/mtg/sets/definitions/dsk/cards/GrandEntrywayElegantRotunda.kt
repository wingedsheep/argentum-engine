package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Grand Entryway // Elegant Rotunda (DSK 15) — split-layout Room (CR 709.5).
 *
 * Grand Entryway {1}{W} — Enchantment — Room
 *   When you unlock this door, create a 1/1 white Glimmer enchantment creature token.
 *
 * Elegant Rotunda {2}{W} — Enchantment — Room
 *   When you unlock this door, put a +1/+1 counter on each of up to two target creatures.
 *
 * Cast each half separately; the cast face enters unlocked, the other locked. Pay the locked
 * face's printed mana cost as a sorcery-speed special action to unlock it (CR 709.5e).
 *
 * Both halves are "when you unlock this door" triggers ([Triggers.OnDoorUnlocked], CR 709.5h).
 * Grand Entryway makes the same 1/1 white Glimmer enchantment-creature token as Tunnel Surveyor
 * ([Effects.CreateToken] with `enchantmentToken = true`). Elegant Rotunda reuses the Byrke /
 * Weftblade "each of up to two target creatures" shape — an optional `count = 2` [TargetCreature]
 * fanned out with [ForEachTargetEffect] so each chosen creature gets one +1/+1 counter.
 */
val GrandEntrywayElegantRotunda = card("Grand Entryway // Elegant Rotunda") {
    layout = CardLayout.SPLIT
    colorIdentity = "W"

    face("Grand Entryway") {
        manaCost = "{1}{W}"
        typeLine = "Enchantment — Room"
        oracleText = "When you unlock this door, create a 1/1 white Glimmer enchantment creature token."

        triggeredAbility {
            trigger = Triggers.OnDoorUnlocked
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Glimmer"),
                enchantmentToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/4/7/475c7449-2c95-4873-94de-68a5e06cdfb8.jpg?1754930946",
            )
        }
    }

    face("Elegant Rotunda") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment — Room"
        oracleText = "When you unlock this door, put a +1/+1 counter on each of up to two target creatures."

        triggeredAbility {
            trigger = Triggers.OnDoorUnlocked
            target("up to two target creatures", TargetCreature(count = 2, optional = true))
            effect = ForEachTargetEffect(
                listOf(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)))
            )
            description = "When you unlock this door, put a +1/+1 counter on each of up to two target creatures."
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Carlos Palma Cruchaga"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef8a8ba7-f955-425d-9a16-816fc48a2a83.jpg?1726867775"
    }
}
