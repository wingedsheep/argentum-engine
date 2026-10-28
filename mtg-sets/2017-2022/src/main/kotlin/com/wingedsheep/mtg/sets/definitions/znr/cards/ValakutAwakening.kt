package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Valakut Awakening {2}{R} // Valakut Stoneforge — Zendikar Rising #174
 * Instant // Land · Modal double-faced card
 *
 * Front — Valakut Awakening: "Put any number of cards from your hand on the bottom of your library,
 * then draw that many cards plus one."
 * Back — Valakut Stoneforge: "This land enters tapped. {T}: Add {R}."
 *
 * The front is the same Gather → Select(any number) → Move → Draw pipeline as Into the Fire's second
 * mode; the draw counts the chosen collection by entity id, so zero cards chosen still draws one.
 */
private val ValakutAwakeningFront = card("Valakut Awakening") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Put any number of cards from your hand on the bottom of your library, then draw that many cards plus one."

    spell {
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val chosen = chooseAnyNumber(hand)
            toLibraryBottom(chosen)
            run(Effects.DrawCards(DynamicAmounts.distinctEntitiesIn(chosen) + 1))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Campbell White"
        flavorText = "\"My ancestors reshaped the world to their vision. I have a few ideas of my own.\"\n—Tars Olan, stoneforge mystic"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/228e551e-023a-4c9a-8f32-58dae6ffdf7f.jpg?1783929350"
    }
}

private val ValakutStoneforgeBack = card("Valakut Stoneforge") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "This land enters tapped.\n{T}: Add {R}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Campbell White"
        flavorText = "\"I can hear the echo of their hammers, long stilled.\"\n—Tars Olan, stoneforge mystic"
        imageUri = "https://cards.scryfall.io/normal/back/2/2/228e551e-023a-4c9a-8f32-58dae6ffdf7f.jpg?1783929350"
    }
}

val ValakutAwakening: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = ValakutAwakeningFront,
    backFace = ValakutStoneforgeBack,
)
