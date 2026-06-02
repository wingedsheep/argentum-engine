package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.CreatureTypePatterns

/**
 * Mistform Wakecaster
 * {4}{U}
 * Creature — Illusion
 * 2/3
 * Flying
 * {1}: This creature becomes the creature type of your choice until end of turn.
 * {2}{U}{U}, {T}: Choose a creature type. Each creature you control becomes that type until end of turn.
 */
val MistformWakecaster = card("Mistform Wakecaster") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion"
    power = 2
    toughness = 3
    oracleText = "Flying\n{1}: This creature becomes the creature type of your choice until end of turn.\n{2}{U}{U}, {T}: Choose a creature type. Each creature you control becomes that type until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}{U}"), Costs.Tap)
        effect = CreatureTypePatterns.becomeChosenTypeAllCreatures(
            controllerOnly = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e5cbfb9-9bd0-4f8b-a444-a480de4b9662.jpg?1562900987"
    }
}
