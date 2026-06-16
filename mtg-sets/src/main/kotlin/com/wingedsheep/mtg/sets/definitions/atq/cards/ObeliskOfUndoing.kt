package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Obelisk of Undoing
 * {1}
 * Artifact
 * {6}, {T}: Return target permanent you both own and control to your hand.
 */
val ObeliskOfUndoing = card("Obelisk of Undoing") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{6}, {T}: Return target permanent you both own and control to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.Tap)
        val permanent = target(
            "target permanent you both own and control",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Permanent.withControllerPredicate(
                        ControllerPredicate.And(
                            listOf(
                                ControllerPredicate.OwnedByYou,
                                ControllerPredicate.ControlledByYou
                            )
                        )
                    )
                )
            )
        )
        effect = Effects.ReturnToHand(permanent)
        description = "{6}, {T}: Return target permanent you both own and control to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Tom Wänerstrand"
        flavorText = "The Battle of Tomakul taught Urza not to rely on fickle reinforcements."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1ba61ccd-4429-4f7c-b9f3-30867878d88e.jpg?1562900796"
    }
}
