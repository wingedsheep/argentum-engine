package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thelon's Chant
 * {1}{G}{G}
 * Enchantment
 * At the beginning of your upkeep, sacrifice this enchantment unless you pay {G}.
 * Whenever a player puts a Swamp onto the battlefield, this enchantment deals 3 damage to that
 * player unless the player puts a -1/-1 counter on a creature they control.
 *
 * The second clause is a punisher, and its teeth are that the way out is a *cost*: a player who
 * controls no creature cannot put the counter anywhere and simply takes the 3. "Puts onto the
 * battlefield" covers every route, not just land drops — a fetched or reanimated Swamp fires it too.
 */
val ThelonsChant = card("Thelon's Chant") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, sacrifice this enchantment unless you pay {G}.\n" +
        "Whenever a player puts a Swamp onto the battlefield, this enchantment deals 3 damage " +
        "to that player unless the player puts a -1/-1 counter on a creature they control."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana("{G}"),
            suffer = SacrificeSelfEffect,
        )
        description = "At the beginning of your upkeep, sacrifice this enchantment unless you pay {G}."
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Land.withSubtype(Subtype.SWAMP),
            binding = TriggerBinding.ANY,
        )
        effect = PayOrSufferEffect(
            cost = Costs.pay.PutCountersOnPermanent(
                counterType = Counters.MINUS_ONE_MINUS_ONE,
                filter = GameObjectFilter.Creature,
            ),
            suffer = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            consequenceDescription = "take 3 damage from this enchantment",
        )
        description = "Whenever a player puts a Swamp onto the battlefield, this enchantment deals 3 damage to that player unless the player puts a -1/-1 counter on a creature they control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d970195-0a09-4cb4-a2c0-c16fcab5c859.jpg?1783947884"
    }
}
