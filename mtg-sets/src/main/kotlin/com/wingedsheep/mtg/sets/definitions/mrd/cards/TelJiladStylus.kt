package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Tel-Jilad Stylus — Mirrodin #260
 * {1} · Artifact
 *
 * {T}: Put target permanent you own on the bottom of your library.
 *
 * "Permanent you **own**", not "you control" — [GameObjectFilter.ownedByYou] is the whole
 * point of the card in its era: it answers a permanent an opponent has stolen (Dominate,
 * Vedalken Shackles, Grab the Reins) by tucking it back into *your* library, and it dodges
 * regeneration and dies-triggers because tucking isn't destruction. Filtering on control
 * instead would invert exactly the case the Stylus exists for.
 *
 * `Zone.LIBRARY` + [ZonePlacement.Bottom] moves the permanent to its **owner's** library
 * bottom, which is "your library" here by construction — the target is one you own. No
 * controller override is needed: ownership, not the current controller, decides the
 * destination zone (CR 400.3).
 */
val TelJiladStylus = card("Tel-Jilad Stylus") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Put target permanent you own on the bottom of your library."

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(
            "permanent",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.ownedByYou()))
        )
        effect = Effects.Move(
            target = permanent,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
        description = "{T}: Put target permanent you own on the bottom of your library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Darrell Riche"
        flavorText = "Etched on Tel-Jilad's trunk is an entire history of Mirrodin—except for an " +
            "expanse near the ground scrubbed smooth by an unknown hand."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/522570eb-e654-4f8a-828c-3e456a0ad8e6.jpg?1783944500"
    }
}
