package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Night Dealings
 * {2}{B}{B}
 * Enchantment
 * Whenever a source you control deals damage to another player, put that many theft counters on this
 * enchantment.
 * {2}{B}{B}, Remove X theft counters from this enchantment: Search your library for a nonland card
 * with mana value X, reveal it, put it into your hand, then shuffle.
 *
 * "Another player" is [Recipient.AnotherPlayer] — a Two-Headed Giant teammate counts, an opponent
 * test would not.
 */
val NightDealings = card("Night Dealings") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a source you control deals damage to another player, put that many theft " +
        "counters on this enchantment.\n" +
        "{2}{B}{B}, Remove X theft counters from this enchantment: Search your library for a nonland " +
        "card with mana value X, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl()).dealsDamage(Recipient.AnotherPlayer)
        effect = Effects.AddDynamicCounters(CounterType.THEFT, DynamicAmounts.triggerDamageAmount(), EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}{B}"),
            Costs.RemoveXCounters(counterType = CounterType.THEFT, self = true),
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Nonland.manaValueEqualsX(),
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58d012ee-9523-469f-8ddb-f4b664093c13.jpg?1783944310"
    }
}
