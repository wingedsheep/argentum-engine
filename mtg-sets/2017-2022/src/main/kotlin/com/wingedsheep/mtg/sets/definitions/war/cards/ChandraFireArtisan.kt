package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Chandra, Fire Artisan
 * {2}{R}{R}
 * Legendary Planeswalker — Chandra
 * Loyalty: 4
 *
 * Whenever one or more loyalty counters are removed from Chandra, she deals that much damage to
 * target opponent or planeswalker.
 * +1: Exile the top card of your library. You may play it this turn.
 * −7: Exile the top seven cards of your library. You may play them this turn.
 *
 * Damage to her, her own [−7] cost and any effect that removes loyalty counters all emit the
 * ordinary loyalty `CountersRemovedEvent`; simultaneous combat damage from several attackers is
 * one removal, so the trigger fires once (ruling).
 */
val ChandraFireArtisan = card("Chandra, Fire Artisan") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 4
    oracleText = "Whenever one or more loyalty counters are removed from Chandra, she deals that much damage " +
        "to target opponent or planeswalker.\n" +
        "+1: Exile the top card of your library. You may play it this turn.\n" +
        "−7: Exile the top seven cards of your library. You may play them this turn."

    triggeredAbility {
        trigger = Triggers.self.losesCounters(CounterType.LOYALTY)
        val victim = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.triggerCountersRemoved(), victim)
        description = "Whenever one or more loyalty counters are removed from Chandra, she deals that much " +
            "damage to target opponent or planeswalker."
    }

    loyaltyAbility(+1) {
        effect = Patterns.Exile.impulse(1)
        description = "Exile the top card of your library. You may play it this turn."
    }

    loyaltyAbility(-7) {
        effect = Patterns.Exile.impulse(7)
        description = "Exile the top seven cards of your library. You may play them this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d21a7b23-8827-49f2-ade4-75a602d17743.jpg?1783933433"
        ruling("2019-05-03", "Chandra's first ability triggers if she loses loyalty from being dealt damage, from her last loyalty ability being activated, or because an effect removes loyalty counters from her. If an effect simply causes Chandra to leave the battlefield, no counters were removed and so her ability doesn't trigger.")
        ruling("2019-05-03", "If multiple creatures deal combat damage to Chandra at the same time, those counters are all removed at once, and her first ability triggers only once.")
        ruling("2019-05-03", "If all of Chandra's loyalty counters are removed, her first ability has her deal damage even though she's left the battlefield.")
    }
}
