package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eater of Hope
 * {5}{B}{B}
 * Creature — Demon
 * 6/4
 * Flying
 * {B}, Sacrifice another creature: Regenerate this creature.
 * {2}{B}, Sacrifice two other creatures: Destroy target creature.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val EaterOfHope = card("Eater of Hope") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 4
    oracleText = "Flying\n" +
        "{B}, Sacrifice another creature: Regenerate this creature.\n" +
        "{2}{B}, Sacrifice two other creatures: Destroy target creature."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.Regenerate(EffectTarget.Self)
        description = "{B}, Sacrifice another creature: Regenerate this creature."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.SacrificeMultiple(2, GameObjectFilter.Creature.notSourceItself()),
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
        description = "{2}{B}, Sacrifice two other creatures: Destroy target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Peter Mohrbacher"
        flavorText = "Gods can be appeased. Demons, however . . ."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f75f3d3f-925d-45ef-92e6-22aca22e5451.jpg?1783939559"
    }
}
