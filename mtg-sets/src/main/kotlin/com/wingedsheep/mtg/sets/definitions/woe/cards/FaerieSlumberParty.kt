package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Faerie Slumber Party
 * {4}{U}{U}
 * Sorcery
 *
 * Return all creatures to their owners' hands. For each opponent who controlled a creature
 * returned this way, you create two 1/1 blue Faerie creature tokens with flying and
 * "This token can block only creatures with flying."
 *
 * Ordering is the whole difficulty. The tokens are creatures, so they must be created *after*
 * the mass bounce (created first, they would be returned themselves and cease to exist), but the
 * payoff counts controllers that the bounce has already erased — `ControllerComponent` is stripped
 * when a permanent leaves the battlefield, and `move` to a hand routes by *owner*, so reading
 * controllers afterwards would answer the wrong question for a stolen creature.
 *
 * So the pipeline snapshots the controllers while the creatures are still in play (the Builder's
 * Bane / Break the Spell shape):
 *
 *   1. `gather(Creature)` — every creature, any controller; this is the set that gets returned.
 *   2. `filterSplit(… opponentControls())` — the opponents' half, evaluated pre-bounce.
 *   3. `captureControllers(theirs)` — a parallel list of controller ids, snapshotted pre-bounce.
 *   4. `move(all, ToZone(HAND))` — battlefield → hand always collapses to the owner's hand.
 *   5. `DistinctEntitiesInCollections(controllers)` de-duplicates that list down to *how many
 *      distinct opponents*, doubled for the two Faeries each.
 *
 * Because the gather is unconditional, "opponents who controlled a creature returned this way" and
 * "opponents who controlled a creature as this resolved" only diverge if some creature fails to
 * leave the battlefield at step 4.
 */
val FaerieSlumberParty = card("Faerie Slumber Party") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all creatures to their owners' hands. For each opponent who controlled " +
        "a creature returned this way, you create two 1/1 blue Faerie creature tokens with " +
        "flying and \"This token can block only creatures with flying.\""

    spell {
        effect = Effects.Pipeline(
            descriptionOverride = "Return all creatures to their owners' hands. For each opponent " +
                "who controlled a creature returned this way, you create two 1/1 blue Faerie " +
                "creature tokens with flying and \"This token can block only creatures with " +
                "flying.\""
        ) {
            val creatures = gather(
                GameObjectFilter.Creature,
                name = "faerieSlumberPartyCreatures",
            )

            // "each opponent who controlled a creature returned this way" — snapshotted while the
            // creatures are still on the battlefield, so control-changing effects are honored.
            val (theirs, _) = filterSplit(
                creatures,
                GameObjectFilter.Creature.opponentControls(),
                name = "faerieSlumberPartyOpponentCreatures",
                restName = "faerieSlumberPartyOwnCreatures",
            )
            val controllers = captureControllers(theirs, name = "faerieSlumberPartyControllers")

            move(creatures, CardDestination.ToZone(Zone.HAND))

            run(
                woeFaerieToken(
                    count = DynamicAmount.Multiply(
                        DynamicAmount.DistinctEntitiesInCollections(listOf(controllers.key)),
                        2,
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "311"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8e5de58-cc4c-40b2-94c8-4e4d7cebfaf8.jpg?1783915040"
    }
}
