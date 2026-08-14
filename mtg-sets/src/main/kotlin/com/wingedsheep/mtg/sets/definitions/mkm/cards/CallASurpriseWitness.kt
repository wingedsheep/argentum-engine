package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Call a Surprise Witness — Murders at Karlov Manor #6
 * {1}{W} · Sorcery
 *
 * Return target creature card with mana value 3 or less from your graveyard to the battlefield.
 * Put a flying counter on it. It's a Spirit in addition to its other types.
 *
 * Two mana of reanimation with an evasion rider. Resolution is a sequential composite (CR 608.2)
 * over one stable target reference, the Abuelo's Awakening shape:
 *  1. [Effects.PutOntoBattlefield] moves the targeted card out of the controller's graveyard. The
 *     bound target survives the zone change, so the two riders land on the new permanent rather
 *     than on a stale graveyard object.
 *  2. A **flying counter** ([Counters.FLYING]) — the keyword counter, not a granted keyword. That
 *     distinction matters: the counter rides along through copy effects and survives an effect
 *     that removes all abilities, and it is removed only by removing the counter.
 *  3. **Spirit** is added with [Effects.AddSubtype] at [Duration.Permanent] — "in addition to its
 *     other types" means additive, so a returned Human Detective becomes a Human Detective Spirit.
 *
 * The mana value check is made on the card in the graveyard as the spell targets it, so a creature
 * card with {X} in its cost has mana value 0 there and is always a legal target.
 */
val CallASurpriseWitness = card("Call a Surprise Witness") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target creature card with mana value 3 or less from your graveyard to " +
        "the battlefield. Put a flying counter on it. It's a Spirit in addition to its other types."

    spell {
        val witness = target(
            "target creature card with mana value 3 or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.IsCreature,
                            CardPredicate.ManaValueAtMost(3),
                        ),
                        controllerPredicate = ControllerPredicate.OwnedByYou,
                    ),
                    zone = Zone.GRAVEYARD,
                )
            )
        )
        effect = Effects.PutOntoBattlefield(witness)
            .then(Effects.AddCounters(Counters.FLYING, 1, witness))
            .then(Effects.AddSubtype("Spirit", target = witness, duration = Duration.Permanent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Julia Metzger"
        flavorText = "\"Why would a little thing like death stop me from running the Syndicate? " +
            "I'm going to be here for a long, long time.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5148def-cf1a-460e-8dfd-856103940892.jpg?1783912928"
    }
}
