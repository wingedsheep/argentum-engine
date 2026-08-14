package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Abyssal Harvester
 * {1}{B}{B}
 * Creature — Demon Warlock
 * 3/2
 *
 * {T}: Exile target creature card from a graveyard that was put there this turn. Create a
 * token that's a copy of it, except it's a Nightmare in addition to its other types. Then
 * exile all other Nightmare tokens you control.
 *
 * Modeling notes:
 *  - "put there this turn" is `StatePredicate.PutIntoGraveyardThisTurn` (`.putIntoGraveyardThisTurn()`),
 *    the zone-agnostic sibling of the LTR Samwise / Lobelia predicate: a card milled, discarded,
 *    or countered into a graveyard qualifies just as much as one that died. Any player's
 *    graveyard is fair game ("a graveyard", not "your graveyard"), so no owner constraint.
 *  - The token copies the *exiled* card (CR 707.2: copiable values are the card's printed
 *    characteristics), read off the target after it lands in exile — the same
 *    exile-then-read-it shape Lobelia Sackville-Baggins uses. CR 400.7j lets the later part
 *    of the same effect find the object the card became in the public zone it moved to.
 *  - "Nightmare in addition to its other types" is `addedSubtypes` (unions) rather than
 *    `overrideSubtypes` (replaces).
 *  - "all *other* Nightmare tokens you control" is snapshotted with the opening `gather`,
 *    before the new token exists. Nothing else can create a Nightmare token between the two
 *    steps of one resolution, so the snapshot is exactly "every Nightmare token except the
 *    one this ability just made" — no exclusion step needed.
 */
val AbyssalHarvester = card("Abyssal Harvester") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon Warlock"
    power = 3
    toughness = 2
    oracleText = "{T}: Exile target creature card from a graveyard that was put there this " +
        "turn. Create a token that's a copy of it, except it's a Nightmare in addition to " +
        "its other types. Then exile all other Nightmare tokens you control."

    activatedAbility {
        cost = Costs.Tap
        val harvested = target(
            "creature card in a graveyard that was put there this turn",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Creature.putIntoGraveyardThisTurn(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.Pipeline {
            // Snapshot the Nightmare tokens that exist *before* the copy is created; those are
            // the "other" ones the last clause exiles.
            val previousNightmares = gather(
                GameObjectFilter.Permanent.token().withSubtype(Subtype.NIGHTMARE).youControl()
            )
            run(Effects.Move(harvested, Zone.EXILE, fromZone = Zone.GRAVEYARD))
            run(
                Effects.CreateTokenCopyOfTarget(
                    target = harvested,
                    addedSubtypes = setOf(Subtype.NIGHTMARE)
                )
            )
            exile(previousNightmares)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Diana Franco"
        flavorText = "\"Till the soil, spare no soul. / Neath the darkness, nightmares grow.\"\n—Children's nursery rhyme"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2e0f538-5825-47e9-883c-3ec6fd5b25ea.jpg"
    }
}
