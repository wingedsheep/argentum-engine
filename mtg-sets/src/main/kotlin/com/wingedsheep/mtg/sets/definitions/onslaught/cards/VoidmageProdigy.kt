package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterSpellEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Voidmage Prodigy
 * {U}{U}
 * Creature — Human Wizard
 * 2/1
 * {U}{U}, Sacrifice a Wizard: Counter target spell.
 * Morph {U}
 */
val VoidmageProdigy = card("Voidmage Prodigy") {
    manaCost = "{U}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "{U}{U}, Sacrifice a Wizard: Counter target spell.\nMorph {U}"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{U}{U}"),
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Wizard"))
        )
        target = Targets.Spell
        effect = CounterSpellEffect
    }

    morph = "{U}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/large/front/7/4/7441e7f9-a326-4f61-b7b1-e0dbed06046f.jpg?1562916487"
    }
}
