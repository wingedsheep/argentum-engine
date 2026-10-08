package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Toph, Earthbending Master
 * {3}{G}
 * Legendary Creature — Human Warrior Ally
 * 2/4
 *
 * Landfall — Whenever a land you control enters, you get an experience counter.
 * Whenever you attack, earthbend X, where X is the number of experience counters you have.
 * (Target land you control becomes a 0/0 creature with haste that's still a land. Put X +1/+1
 * counters on it. When it dies or is exiled, return it to the battlefield tapped.)
 *
 * Experience counters sit on the player ([CounterType.EXPERIENCE], CR 122.1). "Whenever you
 * attack" is the once-per-combat `Triggers.you.attacks()`. The earthbend is the dynamic
 * [Effects.Earthbend] (Rockalanche's form, CR 701.66a), counting X as it resolves — so a land
 * that entered between the trigger and its resolution is counted.
 */
val TophEarthbendingMaster = card("Toph, Earthbending Master") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Warrior Ally"
    power = 2
    toughness = 4
    oracleText = "Landfall — Whenever a land you control enters, you get an experience counter.\n" +
        "Whenever you attack, earthbend X, where X is the number of experience counters you have. " +
        "(Target land you control becomes a 0/0 creature with haste that's still a land. Put X +1/+1 " +
        "counters on it. When it dies or is exiled, return it to the battlefield tapped.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.EXPERIENCE, 1, EffectTarget.Controller)
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(DynamicAmounts.playerCounterCount(CounterType.EXPERIENCE), land)
        description = "Whenever you attack, earthbend X, where X is the number of experience counters you have."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Phima"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22d1b078-9175-45ab-8fbe-ed4af2abde1f.jpg?1783904811"
        ruling("2025-10-02", "All experience counters are identical, no matter how you got them. For example, the last ability will count experience counters that you got from the first ability, from another ability, from another copy of Toph, Earthbending Master, and so on.")
        ruling("2025-10-02", "The experience counter goes on you, the player, not on Toph. You will keep that counter even if Toph, Earthbending Master dies.")
        ruling("2025-10-02", "You may target a land that is already a creature, perhaps because of a previous earthbend ability. The land will get the +1/+1 counters, gain haste, and have its base power and toughness set to 0/0.")
    }
}
