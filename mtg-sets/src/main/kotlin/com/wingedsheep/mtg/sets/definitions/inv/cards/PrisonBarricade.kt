package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prison Barricade
 * {1}{W}
 * Creature — Wall
 * 1/3
 * Defender (This creature can't attack.)
 * Kicker {1}{W} (You may pay an additional {1}{W} as you cast this spell.)
 * If this creature was kicked, it enters with a +1/+1 counter on it and with
 * "This creature can attack as though it didn't have defender."
 *
 * Modeled with the DemonWall pattern: the kicked +1/+1 counter is the persistent
 * marker, and the "can attack despite defender" ability is a static gated on the
 * presence of that counter.
 */
val PrisonBarricade = card("Prison Barricade") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Wall"
    power = 1
    toughness = 3
    oracleText = "Defender (This creature can't attack.)\n" +
        "Kicker {1}{W} (You may pay an additional {1}{W} as you cast this spell.)\n" +
        "If this creature was kicked, it enters with a +1/+1 counter on it and with " +
        "\"This creature can attack as though it didn't have defender.\""

    keywords(Keyword.DEFENDER)
    keywordAbility(KeywordAbility.kicker("{1}{W}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.SourceHasCounter(CounterTypeFilter.Any)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Thomas Gianni"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/449c4800-8718-4593-a61e-03ad7f348c6d.jpg?1562908879"
    }
}
