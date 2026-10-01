package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rustvine Cultivator
 * {G}
 * Creature — Phyrexian Elf Druid
 * 1/2
 *
 * {T}: Put an oil counter on this creature.
 * {T}, Remove an oil counter from this creature: Untap target land.
 */
val RustvineCultivator = card("Rustvine Cultivator") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Elf Druid"
    power = 1
    toughness = 2
    oracleText = "{T}: Put an oil counter on this creature.\n" +
        "{T}, Remove an oil counter from this creature: Untap target land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "{T}: Put an oil counter on this creature."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.OIL, 1))
        val land = target(TargetFilter.Land)
        effect = Effects.Untap(land)
        description = "{T}, Remove an oil counter from this creature: Untap target land."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Lauren K. Cannon"
        flavorText = "\"All elves aspire to be part of nature. Only Phyrexia can truly grant that wish.\"\n—Glissa Sunslayer"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b71fd8f-e688-4210-bc5b-a3f19b5b3497.jpg?1783918010"
    }
}
