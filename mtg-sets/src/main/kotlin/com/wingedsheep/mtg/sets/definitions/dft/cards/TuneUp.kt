package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tune Up — Aetherdrift #33
 * {3}{W} · Sorcery
 *
 * Return target artifact card from your graveyard to the battlefield. If it's a Vehicle, it becomes
 * an artifact creature.
 *
 * Two ordered steps on the same chosen target: [Effects.Move] to the battlefield, then a
 * [ConditionalEffect] that re-reads that target *as a permanent* — [Conditions.TargetMatchesFilter]
 * on the Vehicle subtype — and adds the Creature card type. The animation has no stated duration,
 * so it's [com.wingedsheep.sdk.scripting.Duration.Permanent] (the default of [Effects.AddCardType]):
 * the Vehicle stays a creature for as long as it stays on the battlefield, unlike crew's
 * until-end-of-turn animation.
 *
 * Checking after the move rather than before matters for the type line the client renders — the
 * returned Vehicle becomes `Artifact Creature — Vehicle`. A non-Vehicle artifact just comes back
 * unchanged; if the targeted card has left the graveyard by resolution the spell fizzles on its
 * only target.
 */
val TuneUp = card("Tune Up") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target artifact card from your graveyard to the battlefield. If it's a " +
        "Vehicle, it becomes an artifact creature."

    spell {
        val artifact = target(
            "target artifact card in your graveyard",
            TargetObject(
                filter = TargetFilter(GameObjectFilter.Artifact.ownedByYou(), zone = Zone.GRAVEYARD)
            )
        )
        effect = Effects.Move(artifact, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD).then(
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(
                    GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE)
                ),
                effect = Effects.AddCardType("Creature", artifact),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Chris Rallis"
        flavorText = "\"For most people? The odds would be insurmountable,\" Daretti said.\n" +
            "\"But we're not most people,\" Pia replied."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8bddc5f-8f25-4313-b5bb-e5eae2923878.jpg?1783907912"
    }
}
