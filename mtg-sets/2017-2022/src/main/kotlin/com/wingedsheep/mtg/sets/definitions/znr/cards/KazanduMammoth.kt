package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kazandu Mammoth {1}{G}{G} // Kazandu Valley — Zendikar Rising #189
 * Creature — Elephant 3/3 // Land · Modal double-faced card
 *
 * Front — Kazandu Mammoth: "Landfall — Whenever a land you control enters, this creature gets
 * +2/+2 until end of turn."
 * Back — Kazandu Valley: "This land enters tapped. {T}: Add {G}."
 */
private val KazanduMammothFront = card("Kazandu Mammoth") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant"
    oracleText = "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"It doesn't obey me. It cooperates with me.\"\n—Triel, Kazandu tuskcaller"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f632537-63bf-4490-86e6-e6067b9c1a3b.jpg?1783929346"
    }
}

private val KazanduValleyBack = card("Kazandu Valley") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "This land enters tapped.\n{T}: Add {G}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"The elves stick to the trees. Here, where the trees welcome the sky and leave room for grazing herds—this is our land.\"\n—Triel, Kazandu tuskcaller"
        imageUri = "https://cards.scryfall.io/normal/back/2/f/2f632537-63bf-4490-86e6-e6067b9c1a3b.jpg?1783929346"
    }
}

val KazanduMammoth: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = KazanduMammothFront,
    backFace = KazanduValleyBack,
)
