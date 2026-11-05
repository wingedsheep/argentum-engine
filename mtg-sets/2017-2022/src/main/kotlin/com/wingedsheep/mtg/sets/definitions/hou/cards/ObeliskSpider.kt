package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Obelisk Spider
 * {1}{B}{G}
 * Creature — Spider
 * 1/4
 * Reach
 * Whenever this creature deals combat damage to a creature, put a -1/-1 counter on that creature.
 * Whenever you put one or more -1/-1 counters on a creature, each opponent loses 1 life and you gain 1 life.
 *
 * The first trigger's [EffectTarget.TriggeringEntity] is the damaged creature (as on Voracious
 * Cobra / Sword of Kaldra); it puts exactly one counter regardless of the damage dealt, and does
 * nothing if that creature has already left the battlefield.
 *
 * The second trigger is the per-recipient shape of `getsCounters` (as on Hapatra, Vizier of
 * Poisons): counters put on several creatures at once trigger it once per creature, per the
 * 2017-07-14 ruling. `by = You` scopes it to counters you put — including the first trigger's.
 */
val ObeliskSpider = card("Obelisk Spider") {
    manaCost = "{1}{B}{G}"
    typeLine = "Creature — Spider"
    oracleText = "Reach\n" +
        "Whenever this creature deals combat damage to a creature, put a -1/-1 counter on that creature.\n" +
        "Whenever you put one or more -1/-1 counters on a creature, each opponent loses 1 life and you gain 1 life."
    power = 1
    toughness = 4

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.TriggeringEntity)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).getsCounters(CounterType.MINUS_ONE_MINUS_ONE, by = Player.You)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "YW Tang"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae054604-0d77-45d0-b17b-55a83fb18e39.jpg?1783936011"
        ruling("2017-07-14", "Obelisk Spider's first triggered ability puts only one -1/-1 counter on the creature, no matter how much combat damage it dealt.")
        ruling("2017-07-14", "If Obelisk Spider deals combat damage to a creature but that creature isn't on the battlefield as Obelisk Spider's first triggered ability resolves, that ability won't put a -1/-1 counter on it.")
        ruling("2017-07-14", "If you put one or more -1/-1 counters on each of multiple creatures at the same time, Obelisk Spider's last ability triggers once for each of those creatures.")
    }
}
