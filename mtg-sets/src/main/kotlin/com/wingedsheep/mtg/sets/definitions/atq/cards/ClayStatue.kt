package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clay Statue
 * {4}
 * Artifact Creature — Golem
 * 3/1
 * {2}: Regenerate this creature.
 */
val ClayStatue = card("Clay Statue") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 3
    toughness = 1
    oracleText = "{2}: Regenerate this creature."

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = RegenerateEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Jesper Myrfors"
        flavorText = "Tawnos won fame as Urza's greatest assistant. After he created these warriors, Urza ended his apprenticeship, promoting him directly to the rank of master."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64975352-8d35-4d02-94ac-fa0c6ee12409.jpg?1562916020"
    }
}
