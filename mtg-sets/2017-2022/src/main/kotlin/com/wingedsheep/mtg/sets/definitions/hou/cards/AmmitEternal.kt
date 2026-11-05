package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ammit Eternal — Hour of Devastation #57 (canonical printing)
 * {2}{B} · Creature — Zombie Crocodile Demon · 5/5
 *
 * Afflict 3 (Whenever this creature becomes blocked, defending player loses 3 life.)
 * Whenever an opponent casts a spell, put a -1/-1 counter on this creature.
 * Whenever this creature deals combat damage to a player, remove all -1/-1 counters from it.
 *
 * Afflict is keyword-only: the engine derives the becomes-blocked trigger (see `Afflict`).
 */
val AmmitEternal = card("Ammit Eternal") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie Crocodile Demon"
    power = 5
    toughness = 5
    oracleText = "Afflict 3 (Whenever this creature becomes blocked, defending player loses 3 life.)\n" +
        "Whenever an opponent casts a spell, put a -1/-1 counter on this creature.\n" +
        "Whenever this creature deals combat damage to a player, remove all -1/-1 counters from it."

    afflict(3)

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.RemoveAllCountersOfType(CounterType.MINUS_ONE_MINUS_ONE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b04934f-87d1-4768-b7b5-5f7249a09771.jpg?1783936044"
        ruling("2017-07-14", "Ammit Eternal's middle ability will resolve before the spell that caused it to trigger. The ability will resolve even if that spell is countered.")
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
