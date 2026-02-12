package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost

/**
 * Disruptive Pitmage
 * {2}{U}
 * Creature — Human Wizard
 * 1/1
 * {T}: Counter target spell unless its controller pays {1}.
 * Morph {U}
 */
val DisruptivePitmage = card("Disruptive Pitmage") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}: Counter target spell unless its controller pays {1}.\nMorph {U}"

    activatedAbility {
        cost = AbilityCost.Tap
        target = Targets.Spell
        effect = Effects.CounterUnlessPays("{1}")
    }

    morph = "{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Arnie Swekel"
        flavorText = "\"If ignorance is bliss, wizards should be the most miserable people in the world.\""
        imageUri = "https://cards.scryfall.io/large/front/5/b/5b0d9c2f-356c-4f27-8560-8ffceadac31c.jpg?1562916467"
    }
}
