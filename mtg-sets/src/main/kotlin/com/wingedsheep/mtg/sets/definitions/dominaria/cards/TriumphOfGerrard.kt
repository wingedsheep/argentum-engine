package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Triumph of Gerrard
 * {1}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Put a +1/+1 counter on target creature you control with the greatest power.
 * III — Target creature you control with the greatest power gains flying, first strike,
 *        and lifelink until end of turn.
 */
val TriumphOfGerrard = card("Triumph of Gerrard") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Put a +1/+1 counter on target creature you control with the greatest power.\n" +
        "III — Target creature you control with the greatest power gains flying, first strike, and lifelink until end of turn."

    val greatestPowerTarget = TargetCreature(
        filter = TargetFilter.CreatureYouControl.hasGreatestPower()
    )

    sagaChapter(1) {
        val creature = target("creature you control with the greatest power", greatestPowerTarget)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    sagaChapter(2) {
        val creature = target("creature you control with the greatest power", greatestPowerTarget)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    sagaChapter(3) {
        val creature = target("creature you control with the greatest power", greatestPowerTarget)
        effect = CompositeEffect(listOf(
            Effects.GrantKeyword(Keyword.FLYING, creature),
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature),
            Effects.GrantKeyword(Keyword.LIFELINK, creature)
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3416fae7-46a0-4048-b969-9cf95bac09db.jpg?1562911270"
        ruling("2018-04-27", "If you control more than one creature with the greatest power among creatures you control, you choose one of them as the target.")
        ruling("2018-04-27", "If the target creature no longer has the greatest power as the chapter ability resolves, the ability still resolves and affects that creature.")
    }
}
