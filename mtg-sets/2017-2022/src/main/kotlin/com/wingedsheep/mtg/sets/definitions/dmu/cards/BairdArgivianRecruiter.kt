package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Baird, Argivian Recruiter
 * {R}{W}
 * Legendary Creature — Human Soldier
 * 2/2
 * At the beginning of your end step, if you control a creature with power greater than its base
 * power, create a 1/1 white Soldier creature token.
 *
 * "Base power" is the layers 1–7b value (copy effects, CDAs, "has base power N"), so a creature
 * whose power was *set* doesn't qualify, but one pumped by a counter or an anthem does.
 */
val BairdArgivianRecruiter = card("Baird, Argivian Recruiter") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Soldier"
    oracleText = "At the beginning of your end step, if you control a creature with power greater than its base power, create a 1/1 white Soldier creature token."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouControl(GameObjectFilter.Creature.powerGreaterThanBase())
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Jarel Threat"
        flavorText = "\"Fear doesn't make you a coward; giving in to it does. Now take up your sword and fight!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f643d78-d8d8-4645-9357-a68ebd0d3517.jpg?1783921287"
    }
}
