package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Star Pupil — Strixhaven: School of Mages #30 (canonical printing)
 * {W} · Creature — Human Wizard · 0/0
 *
 * This creature enters with a +1/+1 counter on it.
 * When this creature dies, put its counters on target creature you control.
 *
 * The dies trigger is [Effects.MoveAllLastKnownCounters]: every counter kind it had as it died,
 * read from last-known information, not just its +1/+1 counters.
 */
val StarPupil = card("Star Pupil") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 0
    oracleText = "This creature enters with a +1/+1 counter on it.\n" +
        "When this creature dies, put its counters on target creature you control."

    replacementEffect(EntersWithCounters(count = 1, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.self.dies()
        val recipient = target(TargetFilter.CreatureYouControl)
        effect = Effects.MoveAllLastKnownCounters(recipient)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Howard Lyon"
        flavorText = "\"When I'm done here, they'll name entire buildings after me.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99e9318f-2e17-4b6e-895f-c17cd5d0d282.jpg?1783927385"
        ruling("2021-04-16", "Star Pupil puts all of its counters onto the target creature, not just its +1/+1 counters.")
        ruling("2021-04-16", "Star Pupil's ability doesn't cause you to move counters from itself to the target creature. Rather, you put the same number of each kind of counter Star Pupil had when it died onto that creature.")
        ruling("2021-04-16", "If Star Pupil has -1/-1 counters on it when it dies, the last ability will include those as well. This may result in the recipient also dying.")
    }
}
