package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lakeshore Apothecary — The Hobbit #43
 * {1}{U} · Creature — Human Cleric · Common
 * 1/2
 *
 * Vigilance
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 *
 * The draw trigger is [Triggers.NthCardDrawn] (CR 121.2) — it reads the per-player draw counter and
 * fires exactly once per turn, on the crossing into the second draw, so a single two-card draw fires
 * it once rather than twice. No target: the counter always goes on this creature.
 */
val LakeshoreApothecary = card("Lakeshore Apothecary") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "Vigilance\n" +
        "Whenever you draw your second card each turn, put a +1/+1 counter on this creature."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Wei Guan"
        flavorText = "After Smaug fell, Bard took the lead with the hard task of governing and " +
            "protecting the people. Most would have perished in the winter that now hurried if " +
            "help had not been to hand."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abfbb255-a39b-4df5-bfb6-5298584e89f0.jpg?1785497053"
    }
}
