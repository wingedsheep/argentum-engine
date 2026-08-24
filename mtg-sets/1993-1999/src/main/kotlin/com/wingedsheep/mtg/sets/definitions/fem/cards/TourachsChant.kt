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
 * Tourach's Chant
 * {1}{B}{B}
 * Enchantment
 * At the beginning of your upkeep, sacrifice this enchantment unless you pay {B}.
 * Whenever a player puts a Forest onto the battlefield, this enchantment deals 3 damage to that
 * player unless they put a -1/-1 counter on a creature they control.
 *
 * The second clause is a punisher, and its teeth are that the way out is a *cost*: a player who
 * controls no creature cannot put the counter anywhere and simply takes the 3. "Puts onto the
 * battlefield" covers every route, not just land drops — a fetched or reanimated Forest fires it too.
 */
val TourachsChant = card("Tourach's Chant") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, sacrifice this enchantment unless you pay {B}.\n" +
        "Whenever a player puts a Forest onto the battlefield, this enchantment deals 3 damage " +
        "to that player unless they put a -1/-1 counter on a creature they control."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana("{B}"),
            suffer = SacrificeSelfEffect,
        )
        description = "At the beginning of your upkeep, sacrifice this enchantment unless you pay {B}."
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Land.withSubtype(Subtype.FOREST),
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
        description = "Whenever a player puts a Forest onto the battlefield, this enchantment deals 3 damage to that player unless they put a -1/-1 counter on a creature they control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Richard Kane Ferguson"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06883fd2-eccd-47c6-8c34-10d95e923685.jpg?1783947899"
    }
}
