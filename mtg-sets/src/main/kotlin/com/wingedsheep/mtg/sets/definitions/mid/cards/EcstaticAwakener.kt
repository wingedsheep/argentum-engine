package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Ecstatic Awakener // Awoken Demon (Innistrad: Midnight Hunt). */
private val EcstaticAwakenerFront = card("Ecstatic Awakener") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    oracleText = "{2}{B}, Sacrifice another creature: Draw a card, then transform this creature. Activate only once each turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.Composite(
            Effects.DrawCards(1),
            TransformEffect(EffectTarget.Self)
        )
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Draw a card, then transform this creature. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Tuan Duong Chu"
        flavorText = "\"Great Ormendahl, I kneel before you, a fragment of your will, a servant of your tyranny. I beg you, release the strength within me!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbdad18e-e262-41f9-b252-1cbdcdd1b5f9.jpg?1783925625"
    }
}

private val AwokenDemon = card("Awoken Demon") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Creature — Demon"
    oracleText = ""
    power = 4
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Tuan Duong Chu"
        flavorText = "Bones cracked, flesh split apart, and the screams of euphoric agony gave way to unholy laughter."
        imageUri = "https://cards.scryfall.io/normal/back/b/b/bbdad18e-e262-41f9-b252-1cbdcdd1b5f9.jpg?1783925625"
    }
}

val EcstaticAwakener: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = EcstaticAwakenerFront,
    backFace = AwokenDemon,
)
