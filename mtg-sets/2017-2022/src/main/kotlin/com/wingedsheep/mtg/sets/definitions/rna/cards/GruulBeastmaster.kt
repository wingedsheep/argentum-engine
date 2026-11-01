package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gruul Beastmaster — Ravnica Allegiance #129
 * {3}{G} · Creature — Human Shaman · 1 / 1
 *
 * Riot, plus an attack trigger that lends its power to another creature you control. X is read as
 * the trigger resolves (`sourcePower` falls back to last-known power) and a negative power counts
 * as 0, per the 2019-01-25 rulings.
 */
val GruulBeastmaster = card("Gruul Beastmaster") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    power = 1
    toughness = 1
    oracleText = "Riot (This creature enters with your choice of a +1/+1 counter or haste.)\n" +
        "Whenever this creature attacks, another target creature you control gets +X/+0 until end of turn, " +
        "where X is this creature's power."

    riot()
    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.CreatureYouControl.other())
        val x = DynamicAmounts.nonNegative(DynamicAmounts.sourcePower())
        effect = Effects.ModifyStats(x, DynamicAmounts.fixed(0), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Kimonas Theodossiou"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b0aaded-515f-4ac9-a72f-6948b4d4df51.jpg?1783933671"
        ruling(
            "2019-01-25",
            "The value of X is determined only as Gruul Beastmaster's triggered ability resolves. Once that " +
                "happens, the value of X won't change later in the turn even if Gruul Beastmaster's power changes.",
        )
        ruling(
            "2019-01-25",
            "If Gruul Beastmaster leaves the battlefield before its triggered ability resolves, use its power " +
                "as it last existed on the battlefield to determine the value of X.",
        )
        ruling(
            "2019-01-25",
            "If Gruul Beastmaster's power is negative as its triggered ability resolves, X is considered to be 0. " +
                "In other words, that ability can't cause the creature to lose power.",
        )
    }
}
