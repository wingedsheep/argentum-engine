package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Adaptive Sporesinger — Phyrexia: All Will Be One #157
 * {2}{G}
 * Creature — Phyrexian Druid
 * 2/2
 *
 * Vigilance
 * When this creature enters, choose one —
 * • Target creature gets +2/+2 and gains vigilance until end of turn.
 * • Proliferate.
 */
val AdaptiveSporesinger = card("Adaptive Sporesinger") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Druid"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "When this creature enters, choose one —\n" +
        "• Target creature gets +2/+2 and gains vigilance until end of turn.\n" +
        "• Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Target creature gets +2/+2 and gains vigilance until end of turn") {
                val t = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(2, 2, t) then Effects.GrantKeyword(Keyword.VIGILANCE, t)
            },
            mode("Proliferate") {
                effect = Effects.Proliferate()
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3db6d202-9c6f-4485-9224-173b23de7054.jpg?1783918020"
    }
}
