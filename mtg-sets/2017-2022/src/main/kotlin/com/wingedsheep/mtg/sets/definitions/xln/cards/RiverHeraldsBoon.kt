package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * River Heralds' Boon — Ixalan #204
 * {1}{G} · Instant
 *
 * Put a +1/+1 counter on target creature and a +1/+1 counter on up to one target Merfolk.
 *
 * Two separate instances of the word "target", so one Merfolk may fill both slots and get two
 * counters (the 2017-09-29 ruling). The second slot is optional; left empty, its counter is skipped.
 */
val RiverHeraldsBoon = card("River Heralds' Boon") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature and a +1/+1 counter on up to one target Merfolk."

    spell {
        val creature = target(TargetFilter.Creature)
        val merfolk = target(
            TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.MERFOLK)),
            optional = true,
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, merfolk)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Alex Konstad"
        flavorText = "\"We are kin to the trees, and their strength is our own.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e355a98d-093d-4ab2-84f2-f560d08f1fca.jpg?1783935721"
        ruling(
            "2017-09-29",
            "You can choose one Merfolk creature as both targets for River Heralds' Boon. You can also " +
                "choose two different Merfolk creatures."
        )
    }
}
