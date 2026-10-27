package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lathiel, the Bounteous Dawn — Commander Legends #285
 * {2}{G}{W} · Legendary Creature — Unicorn · 2/2
 *
 * Lifelink
 * At the beginning of each end step, if you gained life this turn, distribute up to that many
 * +1/+1 counters among any number of other target creatures.
 *
 * "Any number of" targets, capped at the life gained (each target needs at least one counter), and
 * an "up to" pool: the division is announced as the trigger goes on the stack, may total less than
 * the life gained, and is frozen there — gaining more life in response changes neither.
 */
val LathielTheBounteousDawn = card("Lathiel, the Bounteous Dawn") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Unicorn"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "At the beginning of each end step, if you gained life this turn, distribute up to that many " +
        "+1/+1 counters among any number of other target creatures."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurn
        target = TargetObject(
            optional = true,
            filter = TargetFilter.OtherCreature,
            dynamicMaxCount = DynamicAmounts.lifeGainedThisTurn(Player.You),
            id = "other target creatures"
        )
        effect = Effects.DistributeCountersAmongTargets(
            DynamicAmounts.lifeGainedThisTurn(Player.You),
            upTo = true,
        )
        description = "At the beginning of each end step, if you gained life this turn, distribute up to " +
            "that many +1/+1 counters among any number of other target creatures."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "285"
        artist = "Lucas Graciano"
        flavorText = "Weeping, the hunter cast aside her bow, struck by the majesty of the creature before her."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29d3484f-abd1-43fe-99f8-e0fa0a7b6692.jpg?1783928769"
    }
}
