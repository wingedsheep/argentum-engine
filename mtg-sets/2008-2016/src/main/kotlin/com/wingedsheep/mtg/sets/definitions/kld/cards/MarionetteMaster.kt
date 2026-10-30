package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Marionette Master
 * {4}{B}{B}
 * Creature — Human Artificer
 * 1/3
 *
 * Fabricate 3 (When this creature enters, put three +1/+1 counters on it or create three 1/1
 * colorless Servo artifact creature tokens.)
 * Whenever an artifact you control is put into a graveyard from the battlefield, target opponent
 * loses life equal to this creature's power.
 *
 * The power is read as the ability resolves; if Marionette Master has left the battlefield by then
 * (it and an artifact went to the graveyard together), `sourcePower()` falls back to its last-known
 * power. A negative power loses no life.
 */
val MarionetteMaster = card("Marionette Master") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Artificer"
    oracleText = "Fabricate 3 (When this creature enters, put three +1/+1 counters on it or create three 1/1 colorless Servo artifact creature tokens.)\n" +
        "Whenever an artifact you control is put into a graveyard from the battlefield, target opponent loses life equal to this creature's power."
    power = 1
    toughness = 3

    keywordAbility(KeywordAbility.fabricate(3))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(DynamicAmounts.sourcePower(), opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/358a87a3-c76e-4c7d-ac84-464894ed9a31.jpg?1783937205"

        ruling("2016-09-20", "The amount of life lost is determined as Marionette Master's last ability resolves. If Marionette Master is no longer on the battlefield, use its power as it last existed on the battlefield to determine how much life is lost.")
        ruling("2016-09-20", "If Marionette Master and an artifact you control are put into a graveyard at the same time, Marionette Master's ability triggers.")
        ruling("2016-09-20", "Artifact tokens that are sacrificed or destroyed are put into their owner's graveyard before ceasing to exist. If you controlled the token, Marionette Master's last ability will trigger.")
    }
}
