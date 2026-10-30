package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eutropia the Twice-Favored
 * {1}{G}{U}
 * Legendary Creature — Human Wizard
 * 2/2
 * Constellation — Whenever an enchantment you control enters, put a +1/+1 counter on target
 * creature. That creature gains flying until end of turn.
 *
 * Setessan Champion's constellation watcher (enchantments you control entering), aimed at a
 * target creature: a +1/+1 counter, then flying until end of turn on the same target.
 */
val EutropiaTheTwiceFavored = card("Eutropia the Twice-Favored") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Constellation — Whenever an enchantment you control enters, put a +1/+1 counter on " +
        "target creature. That creature gains flying until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.FLYING, creature)
        description = "Constellation — Whenever an enchantment you control enters, put a +1/+1 counter " +
            "on target creature. That creature gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Sara Winters"
        flavorText = "Nylea speaks to her in the windblown leaves; Thassa in the rushing tides."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2159949-3d21-448d-bc34-a3dbaf219476.jpg?1783931521"
        ruling(
            "2020-01-24",
            "If an enchantment creature enters the battlefield under your control, Eutropia's ability can " +
                "target that creature."
        )
        ruling("2020-01-24", "Eutropia can be the target of its own triggered ability.")
    }
}
