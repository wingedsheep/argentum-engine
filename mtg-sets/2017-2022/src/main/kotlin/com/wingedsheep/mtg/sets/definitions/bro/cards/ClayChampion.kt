package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clay Champion
 * {X}{4}
 * Artifact Creature — Construct
 * 2/2
 * This creature enters with three +1/+1 counters on it for each {G}{G} spent to cast it.
 * When this creature enters, choose up to two other target creatures you control. For each
 * {W}{W} spent to cast this creature, put a +1/+1 counter on each of them.
 */
val ClayChampion = card("Clay Champion") {
    manaCost = "{X}{4}"
    colorIdentity = "GW"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "This creature enters with three +1/+1 counters on it for each {G}{G} spent to cast it.\n" +
        "When this creature enters, choose up to two other target creatures you control. For each {W}{W} spent to cast this creature, put a +1/+1 counter on each of them."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.manaOfColorSpent(Color.GREEN) / 2 * 3))

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreatureYouControl, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.manaOfColorSpent(Color.WHITE) / 2,
                EffectTarget.ContextTarget(0)
            )
        )
        description = "When this creature enters, choose up to two other target creatures you control. " +
            "For each {W}{W} spent to cast this creature, put a +1/+1 counter on each of them."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "230"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/625f3b20-4b9a-4f61-8afe-31a761711b43.jpg?1783920020"
    }
}
