package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Imagecrafter
 * {U}
 * Creature — Human Wizard
 * 1/1
 * {T}: Choose a creature type other than Wall. Target creature becomes that type until end of turn.
 */
val Imagecrafter = card("Imagecrafter") {
    manaCost = "{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}: Choose a creature type other than Wall. Target creature becomes that type until end of turn."

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", TargetCreature())
        effect = BecomeCreatureTypeEffect(
            target = t,
            excludedTypes = listOf("Wall")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Terese Nielsen"
        flavorText = "\"When Otarians learned not to trust wizards, the wizards learned to adapt.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91be6441-8a45-43e4-8d12-a886dcaadbd3.jpg?1562929336"
    }
}
