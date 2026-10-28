package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Beyeen Veil {1}{U} // Beyeen Coast — Zendikar Rising #46
 * Instant // Land · Modal double-faced card
 *
 * Front — Beyeen Veil: "Creatures your opponents control get -2/-0 until end of turn."
 * Back — Beyeen Coast: "This land enters tapped. {T}: Add {U}."
 */
private val BeyeenVeilFront = card("Beyeen Veil") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Creatures your opponents control get -2/-0 until end of turn."

    spell {
        effect = Patterns.Group.modifyStatsForAll(
            power = -2,
            toughness = 0,
            filter = GroupFilter.AllCreaturesOpponentsControl
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "YW Tang"
        flavorText = "\"All you minnows in the murk, come to my light.\"\n—Jejenar, Silundi lullmage"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f411f08-45dd-4d73-8894-daf51c175150.jpg?1783929412"
    }
}

private val BeyeenCoastBack = card("Beyeen Coast") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "This land enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "YW Tang"
        flavorText = "\"Stay close to my light. You don't want to get lost on the rocks.\"\n—Jejenar, Silundi lullmage"
        imageUri = "https://cards.scryfall.io/normal/back/5/f/5f411f08-45dd-4d73-8894-daf51c175150.jpg?1783929412"
    }
}

val BeyeenVeil: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = BeyeenVeilFront,
    backFace = BeyeenCoastBack,
)
