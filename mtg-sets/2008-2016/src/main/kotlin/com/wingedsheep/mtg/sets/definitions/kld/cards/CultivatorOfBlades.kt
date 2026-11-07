package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cultivator of Blades
 * {3}{G}{G}
 * Creature — Elf Artificer
 * 1/1
 * Fabricate 2
 * Whenever this creature attacks, you may have other attacking creatures get +X/+X until end of
 * turn, where X is this creature's power.
 *
 * Fabricate is engine-derived from [KeywordAbility.fabricate]. The attack trigger is a "may"
 * decided on resolution (per the 2016-09-20 ruling), over the attacking group read at that point
 * minus the Cultivator itself. X is read on resolution — `sourcePower` falls back to the
 * Cultivator's last-known power if it has left the battlefield — and a negative power counts as 0.
 */
val CultivatorOfBlades = card("Cultivator of Blades") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Artificer"
    oracleText = "Fabricate 2 (When this creature enters, put two +1/+1 counters on it or create two 1/1 " +
        "colorless Servo artifact creature tokens.)\n" +
        "Whenever this creature attacks, you may have other attacking creatures get +X/+X until end of turn, " +
        "where X is this creature's power."
    power = 1
    toughness = 1

    keywordAbility(KeywordAbility.fabricate(2))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val x = DynamicAmounts.nonNegative(DynamicAmounts.sourcePower())
        effect = Effects.May(
            Effects.ForEachInGroup(
                GroupFilter.AttackingCreatures.other(),
                Effects.ModifyStats(x, x, EffectTarget.IterationEntity),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d21d35b-1b4e-43aa-8fb7-0dd7a2fa91a1.jpg?1783937180"
        ruling(
            "2017-07-14",
            "If this creature's power is negative as its ability resolves, X is considered to be 0.",
        )
        ruling(
            "2016-09-20",
            "The value of X is determined as Cultivator of Blades's last ability resolves. It won't change " +
                "later, even if its power changes. If Cultivator of Blades is no longer on the battlefield, " +
                "use its power as it last existed on the battlefield to determine the value of X.",
        )
        ruling(
            "2016-09-20",
            "You choose whether or not to have your other attacking creatures get +X/+X as Cultivator of " +
                "Blades's triggered ability resolves, not as you put it onto the stack.",
        )
    }
}
