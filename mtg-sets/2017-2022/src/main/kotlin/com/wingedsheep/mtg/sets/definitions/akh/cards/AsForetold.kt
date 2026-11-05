package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * As Foretold
 * {2}{U}
 * Enchantment
 * At the beginning of your upkeep, put a time counter on this enchantment.
 * Once each turn, you may pay {0} rather than pay the mana cost for a spell you cast with mana
 * value X or less, where X is the number of time counters on this enchantment.
 *
 * "Pay {0} rather than pay the mana cost" is an alternative cost of nothing — casting without
 * paying the mana cost (CR 118.9; additional costs still apply, X is 0). "Once each turn" is any
 * player's turn, so `oncePerTurn` without `duringYourTurnOnly`: an instant or flash spell on an
 * opponent's turn may use it too. X is read live off the enchantment's time counters.
 */
val AsForetold = card("As Foretold") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, put a time counter on this enchantment.\n" +
        "Once each turn, you may pay {0} rather than pay the mana cost for a spell you cast with " +
        "mana value X or less, where X is the number of time counters on this enchantment."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.TIME, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = MayCastWithoutPayingManaCost(
            controllerOnly = true,
            oncePerTurn = true,
            spellFilter = GameObjectFilter.Any.manaValueAtMostDynamic(
                DynamicAmounts.countersOnSelf(CounterType.TIME)
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "42"
        artist = "Tommy Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f91d225-788e-42fc-9d01-8668f672b717.jpg?1783936527"
    }
}
