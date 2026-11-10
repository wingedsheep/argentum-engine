package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thraben Gargoyle // Stonewing Antagonizer (Shadows over Innistrad #266)
 * {1}
 * Artifact Creature — Gargoyle 2/2 // Artifact Creature — Gargoyle Horror 4/2
 *
 * Front — "Defender / {6}: Transform this creature."
 * Back  — "Flying"
 *
 * The flip is an ordinary activated [Effects.Transform] (Ulvenwald Captive's shape). Both faces are
 * colorless.
 */

private val ThrabenGargoyleFront = card("Thraben Gargoyle") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Gargoyle"
    power = 2
    toughness = 2
    oracleText = "Defender\n{6}: Transform this creature."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.Transform(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "266"
        artist = "Daniel Ljunggren"
        flavorText = "Runechanters engrave their strongest wards on those that protect the church."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2687683-2986-4e22-97d9-7820c870475b.jpg?1783937707"
    }
}

private val StonewingAntagonizer = card("Stonewing Antagonizer") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Artifact Creature — Gargoyle Horror"
    power = 4
    toughness = 2
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "266"
        artist = "Daniel Ljunggren"
        flavorText = "Nahiri twisted them in shape and bent them to her will."
        imageUri = "https://cards.scryfall.io/normal/back/f/2/f2687683-2986-4e22-97d9-7820c870475b.jpg?1783937707"
    }
}

val ThrabenGargoyle: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ThrabenGargoyleFront,
    backFace = StonewingAntagonizer,
)
