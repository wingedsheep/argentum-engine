package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Mistform Mutant
 * {4}{U}{U}
 * Creature — Illusion Mutant
 * 3/4
 * {1}{U}: Choose a creature type other than Wall. Target creature becomes that type until end of turn.
 */
val MistformMutant = card("Mistform Mutant") {
    manaCost = "{4}{U}{U}"
    typeLine = "Creature — Illusion Mutant"
    power = 3
    toughness = 4
    oracleText = "{1}{U}: Choose a creature type other than Wall. Target creature becomes that type until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        target = TargetCreature()
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.ContextTarget(0),
            excludedTypes = listOf("Wall")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "John Avon"
        flavorText = "\"Familiarity, the first myth of reality: What you know the best, you observe the least.\""
        imageUri = "https://cards.scryfall.io/large/front/a/2/a25b2697-5d7f-490a-8474-c775096e681e.jpg?1562933313"
    }
}
