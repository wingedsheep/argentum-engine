package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Matzalantli, the Great Door // The Core — The Lost Caverns of Ixalan #256
 * {3} · Legendary Artifact
 * //  · Legendary Land
 *
 * Front — Matzalantli, the Great Door:
 *   {T}: Draw a card, then discard a card.
 *   {4}, {T}: Transform Matzalantli. Activate only if there are four or more permanent types among
 *   cards in your graveyard. (Artifact, battle, creature, enchantment, land, and planeswalker are
 *   permanent types.)
 *
 * Back — The Core:
 *   Fathomless descent — {T}: Add X mana of any one color, where X is the number of permanent cards
 *   in your graveyard.
 *
 * A plain in-place flip (`TransformEffect`, CR 701.27), artifact front / non-creature land back →
 * `CardDefinition.doubleFacedPermanent`. The transform gate reuses the Delirium machinery restricted
 * to permanent cards: `Conditions.DistinctCardTypesInGraveyard(4, GameObjectFilter.Permanent)` —
 * a permanent card only ever carries permanent card types, so distinct card types among the
 * graveyard's permanent cards is exactly its permanent-type count. The back's fathomless-descent
 * mana is `AddAnyColorMana` with a dynamic amount counting permanent cards in the graveyard
 * (`DynamicAmount.Count(You, GRAVEYARD, Permanent)`, the Souls of the Lost / Squirming Emergence
 * count) — one chosen color, X of it.
 */

private val MatzalantliTheGreatDoorFront = card("Matzalantli, the Great Door") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "{T}: Draw a card, then discard a card.\n" +
        "{4}, {T}: Transform Matzalantli. Activate only if there are four or more permanent types " +
        "among cards in your graveyard. (Artifact, battle, creature, enchantment, land, and " +
        "planeswalker are permanent types.)"

    // {T}: Draw a card, then discard a card.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(Effects.DrawCards(1), Effects.Discard(1))
    }

    // {4}, {T}: Transform Matzalantli. Activate only if there are four or more permanent types
    // among cards in your graveyard.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = TransformEffect(EffectTarget.Self)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.DistinctCardTypesInGraveyard(4, GameObjectFilter.Permanent)
            )
        )
        description = "Transform Matzalantli. Activate only if there are four or more permanent " +
            "types among cards in your graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4c31b29-06ba-436d-a3d9-18f4796c39be.jpg?1782694406"
    }
}

private val TheCore = card("The Core") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Matzalantli.)\n" +
        "Fathomless descent — {T}: Add X mana of any one color, where X is the number of permanent " +
        "cards in your graveyard."

    // Fathomless descent — {T}: Add X mana of any one color, X = permanent cards in your graveyard.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Permanent)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/back/b/4/b4c31b29-06ba-436d-a3d9-18f4796c39be.jpg?1782694406"
    }
}

val MatzalantliTheGreatDoor: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = MatzalantliTheGreatDoorFront,
    backFace = TheCore,
)
