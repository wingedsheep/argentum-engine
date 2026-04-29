package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Gnarlbark Elm
 * {2}{B}
 * Creature — Treefolk Warlock
 * 3/4
 *
 * This creature enters with two -1/-1 counters on it.
 * {2}{B}, Remove two counters from this creature: Target creature gets -2/-2 until end of turn.
 * Activate only as a sorcery.
 */
val GnarlbarkElm = card("Gnarlbark Elm") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Treefolk Warlock"
    power = 3
    toughness = 4
    oracleText = "This creature enters with two -1/-1 counters on it.\n" +
        "{2}{B}, Remove two counters from this creature: Target creature gets -2/-2 " +
        "until end of turn. Activate only as a sorcery."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 2,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE, count = 2)
        )
        val creature = target("target creature to weaken", TargetCreature())
        effect = Effects.ModifyStats(-2, -2, creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Loïc Canavaggia"
        flavorText = "It greedily drinks boggart curses and returns their pain in kind."
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e9d65b6-22ff-49f2-8b2a-aeaef91088d3.jpg?1767871866"
    }
}
