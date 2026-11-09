package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ulvenwald Captive // Ulvenwald Abomination (Eldritch Moon #175)
 * {1}{G}
 * Creature — Werewolf Horror 1/2 // Creature — Eldrazi Werewolf 4/6
 *
 * Front — "Defender / {T}: Add {G}. / {5}{G}{G}: Transform this creature."
 * Back  — "{T}: Add {C}{C}."
 *
 * The flip is an ordinary activated [Effects.Transform] (Kessig Prowler's shape); the mana abilities
 * are Llanowar Elves' and Worn Powerstone's. The back is a colorless Eldrazi Werewolf (no mana cost,
 * no color indicator); `colorIdentity` stays "G" because identity is a property of the whole card.
 */

private val UlvenwaldCaptiveFront = card("Ulvenwald Captive") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Werewolf Horror"
    power = 1
    toughness = 2
    oracleText = "Defender\n{T}: Add {G}.\n{5}{G}{G}: Transform this creature."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{5}{G}{G}")
        effect = Effects.Transform(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Chris Rahn"
        flavorText = "\"Even on a plane as dark as this one, nature will organize a defense against the " +
            "unnatural.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dbaef61-fa39-4ea7-bc21-445401c373e7.jpg?1783937442"
    }
}

private val UlvenwaldAbomination = card("Ulvenwald Abomination") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Werewolf"
    power = 4
    toughness = 6
    oracleText = "{T}: Add {C}{C}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Chris Rahn"
        flavorText = "\"The Eldrazi are all too practiced at twisting natural into unnatural.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/back/0/d/0dbaef61-fa39-4ea7-bc21-445401c373e7.jpg?1783937442"
    }
}

val UlvenwaldCaptive: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = UlvenwaldCaptiveFront,
    backFace = UlvenwaldAbomination,
)
