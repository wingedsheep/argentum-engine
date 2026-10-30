package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ironscale Hydra
 * {3}{G}{G}
 * Creature — Hydra
 * 5/5
 * If a creature would deal combat damage to this creature, prevent that damage and put a +1/+1
 * counter on this creature.
 *
 * A prevention replacement with an amount-free rider (the Nine Lives shape): one counter per
 * creature's damage event, however much it would have dealt; and when the damage can't be
 * prevented the rider still lands, so the counter arrives with the damage, before state-based
 * actions check lethal damage (both rulings).
 */
val IronscaleHydra = card("Ironscale Hydra") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 5
    toughness = 5
    oracleText = "If a creature would deal combat damage to this creature, prevent that damage and put " +
        "a +1/+1 counter on this creature."

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Creature,
                damageType = DamageType.Combat
            ),
            onPrevented = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "296"
        artist = "Brian Valeza"
        flavorText = "\"Put down your bows, you fools! Arrows only make it angry!\"\n—Callia, Hyrax Tower scout"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0215cbbf-7bac-4ff7-bceb-23d728797848.jpg?1783931487"
        ruling(
            "2020-01-24",
            "You put only one +1/+1 counter on Ironscale Hydra per creature whose combat damage is prevented, " +
                "no matter how much damage would have been dealt."
        )
        ruling(
            "2020-01-24",
            "If damage that can't be prevented is dealt to Ironscale Hydra, the damage is dealt and you'll " +
                "simultaneously put a +1/+1 counter on it, before the game checks for lethal damage."
        )
    }
}
