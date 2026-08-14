package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Skeleton Shard — Mirrodin #242 (canonical printing; Planechase 2009 is a later reprint)
 * {3} · Artifact
 *
 * {3}, {T} or {B}, {T}: Return target artifact creature card from your graveyard to your hand.
 *
 * Same "pay one cost or the other" template as its cycle-mate Granite Shard — see that card's KDoc
 * for why two `activatedAbility` blocks are the faithful model rather than one ability with a
 * synthetic either-cost atom. Both alternatives include the shard's own {T}, so they compete for the
 * same tap.
 *
 * The target is a card in *your* graveyard (`ownedByYou()` — a graveyard card's owner is its
 * graveyard's player, CR 108.3) that is both an artifact and a creature card.
 */
private val ArtifactCreatureInYourGraveyard = TargetFilter(
    baseFilter = GameObjectFilter.ArtifactCreature.ownedByYou(),
    zone = Zone.GRAVEYARD,
)

val SkeletonShard = card("Skeleton Shard") {
    manaCost = "{3}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{3}, {T} or {B}, {T}: Return target artifact creature card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val card = target(
            "target artifact creature card from your graveyard",
            TargetObject(filter = ArtifactCreatureInYourGraveyard),
        )
        effect = Effects.ReturnToHand(card)
        description = "{3}, {T}: Return target artifact creature card from your graveyard to your hand."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        val card = target(
            "target artifact creature card from your graveyard",
            TargetObject(filter = ArtifactCreatureInYourGraveyard),
        )
        effect = Effects.ReturnToHand(card)
        description = "{B}, {T}: Return target artifact creature card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Doug Chaffee"
        flavorText = "Metal permeates the marrow of every bone in Mephidross—except one."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeffcd61-ea1f-4ccd-b3d3-79efa9d4a0cf.jpg?1783944504"
        ruling("2004-10-04", "You can pay either of the two costs (but not both at the same time) to activate the ability.")
    }
}
