package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seasoned Warrenguard
 * {W}
 * Creature — Rabbit Warrior
 * 1/2
 * Whenever this creature attacks while you control a token, this creature gets +2/+0 until end of turn.
 *
 * Ruling (2024-07-26): If you controlled at least one token when you declared Seasoned Warrenguard
 * as an attacker, it doesn't matter whether or not you control any tokens as its ability resolves.
 */
val SeasonedWarrenguard = card("Seasoned Warrenguard") {
    manaCost = "{W}"
    typeLine = "Creature — Rabbit Warrior"
    power = 1
    toughness = 2
    oracleText = "Whenever this creature attacks while you control a token, this creature gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Token)
        effect = ConditionalEffect(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Token),
            effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Dave Kendall"
        flavorText = "Never underestimate what a rabbit can do with a root vegetable and some moxie."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90873995-876f-4e89-8bc7-41a74f4d931f.jpg?1721425942"
    }
}
