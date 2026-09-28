package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Botanical Brawler
 * {G}{W}
 * Creature — Elemental Warrior
 * 0/0
 * Trample
 * This creature enters with two +1/+1 counters on it.
 * Whenever one or more +1/+1 counters are put on another permanent you control, if it's the first
 * time +1/+1 counters have been put on that permanent this turn, put a +1/+1 counter on this
 * creature.
 *
 * The "first time" window is scoped to the trigger's counter kind: `getsCounters(type =
 * PLUS_ONE_PLUS_ONE, firstTimeEachTurn = true)` asks whether a +1/+1 counter had already been put
 * on that permanent this turn, so an earlier shield or oil counter doesn't close it.
 */
val BotanicalBrawler = card("Botanical Brawler") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elemental Warrior"
    power = 0
    toughness = 0
    oracleText = "Trample\n" +
        "This creature enters with two +1/+1 counters on it.\n" +
        "Whenever one or more +1/+1 counters are put on another permanent you control, if it's " +
        "the first time +1/+1 counters have been put on that permanent this turn, put a +1/+1 " +
        "counter on this creature."

    keywords(Keyword.TRAMPLE)
    replacementEffect(EntersWithCounters(count = 2, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.youControl())
            .getsCounters(type = CounterType.PLUS_ONE_PLUS_ONE, firstTimeEachTurn = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever one or more +1/+1 counters are put on another permanent you " +
            "control, if it's the first time +1/+1 counters have been put on that permanent " +
            "this turn, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/977c15c9-2aad-4d1d-86a6-1f9d78ad7af6.jpg?1783916956"
        ruling(
            "2023-04-14",
            "For each other permanent you control, Botanical Brawler will count +1/+1 counters having been put on that permanent at any time during that turn, even if Botanical Brawler wasn't on the battlefield at that time. For example, if a +1/+1 counter is put on a creature you control, then you cast Botanical Brawler, then another +1/+1 counter is put on that first creature, Botanical Brawler's last ability won't trigger."
        )
        ruling(
            "2023-04-14",
            "Whenever another permanent enters the battlefield under your control with one or more +1/+1 counters on it, Botanical Brawler's last ability will trigger."
        )
    }
}
