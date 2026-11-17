package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Living Totem
 * {3}{G}
 * Creature — Plant Elemental
 * 2/3
 * Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)
 * When this creature enters, you may put a +1/+1 counter on another target creature.
 */
val LivingTotem = card("Living Totem") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    power = 2
    toughness = 3
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "When this creature enters, you may put a +1/+1 counter on another target creature."

    keywords(Keyword.CONVOKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.other())
        optional = true
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca21890c-68be-4929-913e-d28ace6b40a7.jpg"
    }
}
