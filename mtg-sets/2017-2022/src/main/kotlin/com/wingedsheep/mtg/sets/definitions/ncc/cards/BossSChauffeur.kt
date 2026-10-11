package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boss's Chauffeur — {4}{W} Creature — Elf Citizen 0/0 (New Capenna Commander #13).
 *
 * This creature enters with a number of +1/+1 counters on it equal to one plus the number of
 * other creatures you control.
 * Alliance — Whenever another creature you control enters, put a +1/+1 counter on this creature.
 * When this creature dies, create a 1/1 green and white Citizen creature token for each +1/+1
 * counter on it.
 *
 * The entry count is evaluated before the Chauffeur is on the battlefield, so
 * `creaturesYouControl()` already means "other creatures" (the Sheriff of Safe Passage shape).
 * The dies trigger reads the last-known +1/+1 counter count.
 */
val BossSChauffeur = card("Boss's Chauffeur") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elf Citizen"
    power = 0
    toughness = 0
    oracleText = "This creature enters with a number of +1/+1 counters on it equal to one plus the number of other creatures you control.\n" +
        "Alliance — Whenever another creature you control enters, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, create a 1/1 green and white Citizen creature token for each +1/+1 counter on it."

    replacementEffect(
        EntersWithDynamicCounters(
            count = 1 + DynamicAmounts.creaturesYouControl()
        )
    )

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.lastKnownPlusOneCounters(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Citizen"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Yangtian Li"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/147a0ccf-3408-48c7-b4a8-6d81aed39f18.jpg?1783923377"
    }
}
