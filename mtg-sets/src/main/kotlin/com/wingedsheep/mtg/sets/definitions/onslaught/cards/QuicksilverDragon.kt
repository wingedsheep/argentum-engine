package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Quicksilver Dragon
 * {4}{U}{U}
 * Creature — Dragon
 * 5/5
 * Flying
 * {U}: If target spell has only one target and that target is this creature, change that spell's target to another creature.
 * Morph {4}{U}
 */
val QuicksilverDragon = card("Quicksilver Dragon") {
    manaCost = "{4}{U}{U}"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n{U}: If target spell has only one target and that target is this creature, change that spell's target to another creature.\nMorph {4}{U}"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{U}")
        target = Targets.Spell
        effect = Effects.ChangeSpellTarget(targetMustBeSource = true)
    }

    morph = "{4}{U}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Ron Spears"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e93577bd-2711-443c-aa88-a235345d7800.jpg?1562950545"
    }
}
