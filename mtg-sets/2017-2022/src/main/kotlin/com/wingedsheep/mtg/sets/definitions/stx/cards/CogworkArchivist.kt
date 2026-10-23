package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cogwork Archivist — Strixhaven: School of Mages #254 (canonical printing)
 * {6} · Artifact Creature — Construct · 4/5
 *
 * Reach
 * {2}, {T}: Put target card from a graveyard on the bottom of its owner's library.
 *
 * The Junktroller shape with a mana component: any card in any graveyard
 * ([TargetFilter.CardInGraveyard]) moved to [Zone.LIBRARY] at [ZonePlacement.Bottom] — a card
 * moved to a library always lands in its owner's.
 */
val CogworkArchivist = card("Cogwork Archivist") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    oracleText = "Reach\n" +
        "{2}, {T}: Put target card from a graveyard on the bottom of its owner's library."
    power = 4
    toughness = 5

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val card = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(card, Zone.LIBRARY, ZonePlacement.Bottom)
        description = "{2}, {T}: Put target card from a graveyard on the bottom of its owner's library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Joseph Meehan"
        flavorText = "\"It's the largest library in the Multiverse. Of course it has the largest " +
            "librarians.\"\n—Kasmina"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c024a2d-20fb-478b-a62e-e3590a1e3ac1.jpg?1783927281"
    }
}
