package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Phyrexian Archivist
 * {6}
 * Artifact Creature — Phyrexian Construct
 * 4/5
 * Reach
 * {2}, {T}: Put target card from a graveyard on the bottom of its owner's library.
 */
val PhyrexianArchivist = card("Phyrexian Archivist") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Phyrexian Construct"
    oracleText = "Reach\n{2}, {T}: Put target card from a graveyard on the bottom of its owner's library."
    power = 4
    toughness = 5

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val cardInGraveyard = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(
            target = cardInGraveyard,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
        description = "{2}, {T}: Put target card from a graveyard on the bottom of its owner's library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "262"
        artist = "Andreas Zafiratos"
        flavorText = "Silence quickly went from an annoying library rule to an imperative for survival."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93960b5e-b13c-4f7d-a826-8aad6bd210b4.jpg?1783916935"
    }
}
