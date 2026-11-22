package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Vastwood Hydra
 * {X}{G}{G}
 * Creature — Hydra
 * 0/0
 * This creature enters with X +1/+1 counters on it.
 * When this creature dies, you may distribute a number of +1/+1 counters equal to the number of
 * +1/+1 counters on this creature among any number of creatures you control.
 *
 * The dies ability doesn't target (current Oracle): the division is made at resolution among the
 * creatures you control, and the count is the hydra's last-known +1/+1 counters.
 */
val VastwoodHydra = card("Vastwood Hydra") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "When this creature dies, you may distribute a number of +1/+1 counters equal to the number of " +
        "+1/+1 counters on this creature among any number of creatures you control."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(
            Effects.DistributeCountersAmongFiltered(
                DynamicAmounts.lastKnownPlusOneCounters(),
                filter = GameObjectFilter.Creature.youControl(),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e635174-7f7d-4c04-a6aa-8674da6863ff.jpg?1783939898"
        ruling("2013-07-01", "The ability that triggers when Vastwood Hydra dies doesn't target any of the creatures. You can choose to put counters on a creature with protection from green, for example.")
        ruling("2013-07-01", "You choose how the counters will be distributed when the ability resolves.")
        ruling("2013-07-01", "In some unusual cases, Vastwood Hydra will have enough -1/-1 counters put on it to make its toughness 0 or less. If this happens, the ability will still distribute a number of +1/+1 counters equal to the number of +1/+1 counters on Vastwood Hydra before the -1/-1 counters were put on it. For example, if Vastwood Hydra has two +1/+1 counters on it and three -1/-1 counters are put on it, two +1/+1 counters will be distributed.")
    }
}
