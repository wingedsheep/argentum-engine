package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clay Revenant
 * {1}
 * Artifact Creature — Golem
 * 1/2
 * This creature enters tapped.
 * {2}{B}: Return this card from your graveyard to your hand.
 */
val ClayRevenant = card("Clay Revenant") {
    manaCost = "{1}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Golem"
    power = 1
    toughness = 2
    oracleText = "This creature enters tapped.\n" +
        "{2}{B}: Return this card from your graveyard to your hand."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        description = "{2}{B}: Return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Filipe Pagliuso"
        flavorText = "Tawnos built his clay statues for durability, not knowing they'd keep fighting long after the war."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc295bac-5031-46c8-8d9a-368656bcf6d3.jpg"
    }
}
