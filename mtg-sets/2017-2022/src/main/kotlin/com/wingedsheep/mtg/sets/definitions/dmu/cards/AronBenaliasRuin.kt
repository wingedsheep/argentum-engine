package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val AronBenaliasRuin = card("Aron, Benalia's Ruin") {
    manaCost = "{W}{W}{B}"
    typeLine = "Legendary Creature — Phyrexian Human"
    power = 3
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n{W}{B}, {T}, Sacrifice another creature: Put a +1/+1 counter on each creature you control."

    keywords(Keyword.MENACE)
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}{B}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Mark Winters"
        flavorText = "\"He may look like my father, but the man I knew and loved is gone.\"\n—Danitha"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1f8bf98-8911-4c86-978a-427377700544.jpg?1783921288"
    }
}
