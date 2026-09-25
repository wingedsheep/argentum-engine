package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reito Lantern
 * {2}
 * Artifact
 * {3}: Put target card from a graveyard on the bottom of its owner's library.
 *
 * Chrome Companion's graveyard ability without the {T}, so it can be activated repeatedly.
 */
val ReitoLantern = card("Reito Lantern") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}: Put target card from a graveyard on the bottom of its owner's library."

    activatedAbility {
        cost = Costs.Mana("{3}")
        val cardInGraveyard = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(
            target = cardInGraveyard,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
        description = "{3}: Put target card from a graveyard on the bottom of its owner's library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "267"
        artist = "Greg Hildebrandt"
        flavorText = "Lanterns carved from the mystic stones of the Reito Mines were said to light the way of lost souls."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90eba910-852b-47e0-b4cd-07f30d80f921.jpg?1783944276"
    }
}
