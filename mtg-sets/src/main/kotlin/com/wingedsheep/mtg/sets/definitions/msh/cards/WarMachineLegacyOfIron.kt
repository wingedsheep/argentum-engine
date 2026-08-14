package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * War Machine, Legacy of Iron — Marvel Super Heroes #238 (uncommon)
 * {2}{R/W} · Legendary Artifact Creature — Human Hero · 1/3
 *
 * Flying
 * At the beginning of combat on your turn, another target creature you control gets +X/+0 until
 * end of turn, where X is War Machine's power.
 *
 * [Triggers.BeginCombat] is the `StepEvent(BEGIN_COMBAT, Player.You)` shape — "on your turn" is
 * baked into the trigger, so no extra condition is needed. "Another target creature you control"
 * is [Targets.OtherCreatureYouControl] (self-exclusion is part of the filter, so War Machine can
 * never pump himself, and the trigger simply has no legal target when he's your only creature).
 *
 * The pump amount is read at *resolution* from War Machine's projected power
 * ([DynamicAmounts.sourcePower]), matching "where X is War Machine's power" — a pump or an
 * Equipment that lands in response is counted; if he leaves the battlefield before resolution the
 * ability still resolves off his last-known power.
 */
val WarMachineLegacyOfIron = card("War Machine, Legacy of Iron") {
    manaCost = "{2}{R/W}"
    colorIdentity = "RW"
    typeLine = "Legendary Artifact Creature — Human Hero"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "At the beginning of combat on your turn, another target creature you control gets +X/+0 " +
        "until end of turn, where X is War Machine's power."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val boosted = target("another target creature you control", Targets.OtherCreatureYouControl)
        effect = Effects.ModifyStats(
            DynamicAmounts.sourcePower(),
            DynamicAmount.Fixed(0),
            boosted
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Carlos Dattoli"
        flavorText = "\"Listen up, you *screw-heads*. You're history!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/3/433a7d97-9c0b-4f5c-85ad-b55342c02d22.jpg?1783902894"
    }
}
