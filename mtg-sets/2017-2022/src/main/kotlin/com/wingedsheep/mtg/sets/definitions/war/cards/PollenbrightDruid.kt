package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pollenbright Druid
 * {1}{G}
 * Creature — Elf Druid
 * 1/1
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on target creature.
 * • Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 */
val PollenbrightDruid = card("Pollenbright Druid") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on target creature.\n" +
        "• Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Put a +1/+1 counter on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            },
            Mode.noTarget(Effects.Proliferate(), "Proliferate"),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b0f478c-54f8-4087-ad52-d089e2049dda.jpg?1783933409"
    }
}
