package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Caldaia Strongarm
 * {4}{G}
 * Creature — Human Warrior
 * 2/3
 * When this creature enters, put two +1/+1 counters on target creature.
 * Blitz {3}{G} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 */
val CaldaiaStrongarm = card("Caldaia Strongarm") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "When this creature enters, put two +1/+1 counters on target creature.\nBlitz {3}{G} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, t)
    }

    keywordAbility(KeywordAbility.blitz("{3}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58b431d2-33c1-49d8-b45d-690c174bb456.jpg?1783923106"
    }
}
