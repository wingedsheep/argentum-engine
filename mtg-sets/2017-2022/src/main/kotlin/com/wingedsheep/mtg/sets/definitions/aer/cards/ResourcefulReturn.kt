package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Resourceful Return
 * {1}{B}
 * Sorcery
 * Return target creature card from your graveyard to your hand. If you control an artifact, draw a card.
 */
val ResourcefulReturn = card("Resourceful Return") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to your hand. If you control an artifact, draw a card."

    spell {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creatureCard) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Artifact),
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Titus Lunter"
        flavorText = "When the salvage team brought in the shattered gearhulk, a wave of excitement swept through the renegade foundry."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32f57d0b-ab6a-4074-885d-678659729b8a.jpg"
        ruling("2017-02-09", "You can't cast Resourceful Return without a target creature card in your graveyard.")
        ruling("2017-02-09", "Whether or not you control an artifact is checked as Resourceful Return resolves.")
    }
}
