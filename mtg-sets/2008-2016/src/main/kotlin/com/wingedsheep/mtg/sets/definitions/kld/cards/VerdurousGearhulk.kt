package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Verdurous Gearhulk
 * {3}{G}{G}
 * Artifact Creature — Construct
 * 4/4
 * Trample
 * When this creature enters, distribute four +1/+1 counters among any number of target creatures
 * you control.
 *
 * Same shape as Jugan, the Rising Star: "any number of" is up to four targets (CR 601.2d — no more
 * targets than counters), zero allowed. With two or more targets the division is announced as the
 * trigger goes on the stack (CR 603.3d) and an illegal target's share is lost, not re-divided.
 * The Gearhulk itself is a legal target.
 */
val VerdurousGearhulk = card("Verdurous Gearhulk") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "Trample\nWhen this creature enters, distribute four +1/+1 counters among any number of " +
        "target creatures you control."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.CreatureYouControl, count = 4, minCount = 0)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 4)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "172"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0130b99e-5b2f-4482-b141-752e59b72c31.jpg?1783937171"
        ruling(
            "2016-09-20",
            "You choose how the counters will be distributed as you put Verdurous Gearhulk's triggered " +
                "ability onto the stack. Each target creature must be assigned at least one counter."
        )
        ruling(
            "2016-09-20",
            "If one of the target creatures becomes an illegal target in response to Verdurous Gearhulk's " +
                "triggered ability, the +1/+1 counters that would have been put on that creature are lost. " +
                "They can't be put on another legal target."
        )
        ruling("2016-09-20", "Verdurous Gearhulk can be the target of its own triggered ability.")
    }
}
