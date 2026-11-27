package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vessel of Endless Rest
 * {3}
 * Artifact
 * When this artifact enters, put target card from a graveyard on the bottom of its owner's library.
 * {T}: Add one mana of any color.
 */
val VesselOfEndlessRest = card("Vessel of Endless Rest") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, put target card from a graveyard on the bottom of its owner's library.\n{T}: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val cardInGraveyard = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(
            target = cardInGraveyard,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec733373-3f68-47ad-ac35-6f39092f1e26.jpg?1783940650"
    }
}
