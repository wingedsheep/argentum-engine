package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.TriggeredAbilityBuilder
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect

/**
 * Benthic Criminologists — Murders at Karlov Manor #40
 * {4}{U} · Creature — Merfolk Wizard · 4/5
 *
 * Whenever this creature enters or attacks, you may sacrifice an artifact. If you do, draw a card.
 *
 * "Enters or attacks" is two separate triggered abilities sharing one rider — the engine has no
 * combined trigger, and modelling it as two abilities is also what the rules describe (each
 * condition puts its own copy of the ability on the stack).
 *
 * "You may sacrifice an artifact. If you do, draw a card" is an [OptionalCostEffect], not a
 * `MayEffect` around a composite: the draw is gated on the sacrifice actually happening, so
 * declining — or controlling no artifact at all — draws nothing. The Clue tokens this set showers
 * on a blue deck are the intended fuel, and sacrificing a Clue this way is a sacrifice for *this*
 * ability's cost, not an activation of the Clue's own draw ability.
 */
val BenthicCriminologists = card("Benthic Criminologists") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Whenever this creature enters or attacks, you may sacrifice an artifact. If you " +
        "do, draw a card."
    power = 4
    toughness = 5

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        sacrificeAnArtifactToDraw()
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        sacrificeAnArtifactToDraw()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Johan Grenier"
        flavorText = "Scientists from the Simic Combine are regularly seconded to various " +
            "detective agencies in hopes that sharing their expertise will improve the guild's " +
            "tarnished reputation."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/283b6b5a-acdf-4255-a294-0964d9c62686.jpg?1783912916"
    }
}

/** The rider shared by the enters and attacks triggers. */
private fun TriggeredAbilityBuilder.sacrificeAnArtifactToDraw() {
    effect = OptionalCostEffect(
        cost = SacrificeEffect(filter = GameObjectFilter.Artifact),
        ifPaid = Effects.DrawCards(1)
    )
    description = "You may sacrifice an artifact. If you do, draw a card."
}
