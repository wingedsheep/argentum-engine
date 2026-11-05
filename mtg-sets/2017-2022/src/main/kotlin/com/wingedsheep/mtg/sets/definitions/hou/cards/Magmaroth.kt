package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Magmaroth (HOU #102)
 * {3}{R}  Creature — Elemental  5/5
 *
 * At the beginning of your upkeep, put a -1/-1 counter on this creature.
 * Whenever you cast a noncreature spell, remove a -1/-1 counter from this creature.
 */
val Magmaroth = card("Magmaroth") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "At the beginning of your upkeep, put a -1/-1 counter on this creature.\n" +
        "Whenever you cast a noncreature spell, remove a -1/-1 counter from this creature."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.RemoveCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Yeong-Hao Han"
        flavorText = "Sand into glass and flesh into ash—such are the markers of its domain."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76f87128-2d12-467b-895d-943f318b7c38.jpg?1783936027"
        ruling("2017-07-14", "Magmaroth’s second ability resolves before the spell that caused it to trigger. The ability will resolve even if that spell is countered.")
    }
}
