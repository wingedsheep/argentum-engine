package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bala Ged Recovery {2}{G} // Bala Ged Sanctuary — Zendikar Rising #180
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Bala Ged Recovery: "Return target card from your graveyard to your hand."
 * Back — Bala Ged Sanctuary: "This land enters tapped. {T}: Add {G}."
 */
private val BalaGedRecoveryFront = card("Bala Ged Recovery") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return target card from your graveyard to your hand."

    spell {
        val t = target(TargetFilter.CardInGraveyard.ownedByYou())
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Lucas Staniec"
        flavorText = "\"Always have a backup plan.\"\n—Doral, Bala Ged guide"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5cb3052-358d-44a7-8cfd-cd31b236494a.jpg?1783929347"
    }
}

private val BalaGedSanctuaryBack = card("Bala Ged Sanctuary") {
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Lucas Staniec"
        flavorText = "\"I know it's around here somewhere.\"\n—Doral, Bala Ged guide"
        imageUri = "https://cards.scryfall.io/normal/back/c/5/c5cb3052-358d-44a7-8cfd-cd31b236494a.jpg?1783929347"
    }
}

val BalaGedRecovery: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = BalaGedRecoveryFront,
    backFace = BalaGedSanctuaryBack,
)
