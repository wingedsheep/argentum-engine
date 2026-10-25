package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guild Thief (AFR #61) — {1}{U} Creature — Orc Rogue 1/1 (uncommon)
 *
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 * Cunning Action — {3}{U}: This creature can't be blocked this turn.
 */
val GuildThief = card("Guild Thief") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Orc Rogue"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.\n" +
        "Cunning Action — {3}{U}: This creature can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "Cunning Action — {3}{U}: This creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Mike Jordana"
        flavorText = "She puts some coin in the Guild's pocket and sends a message at the same time."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd4348ec-fdb1-4c3b-bec5-42513c6c0662.jpg?1783926512"
    }
}
