package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Atlantean Cavalry
 * {2}{U}
 * Creature — Merfolk Soldier
 * 3/2
 *
 * Vigilance
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 *
 * The draw trigger is [Triggers.NthCardDrawn] (CR 121.2) — it fires exactly once per turn, on the
 * crossing into the second draw, so a single two-card draw fires it once rather than twice.
 */
val AtlanteanCavalry = card("Atlantean Cavalry") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Soldier"
    power = 3
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
        collectorNumber = "45"
        artist = "Eglė Mosakaitė"
        flavorText = "\"These surface dwellers feel safe with their feet on dry land. Let us remind " +
            "them why they fear the deep water.\"\n—Namor the Sub-Mariner"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1319bf54-8705-4cc4-8d08-40f05fd09837.jpg?1783902961"
    }
}
