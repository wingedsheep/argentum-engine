package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Saheeli's Lattice // Mastercraft Raptor (CR 702.167, The Lost Caverns of Ixalan)
 * {1}{R}
 * Artifact // Artifact Creature — Dinosaur
 *
 * Front face — Saheeli's Lattice ({1}{R}, Artifact)
 *   When this artifact enters, you may discard a card. If you do, draw two cards.
 *   Craft with one or more Dinosaurs {4}{R} ({4}{R}, Exile this artifact, Exile
 *   one or more Dinosaurs you control and/or Dinosaur cards from your graveyard:
 *   Return this card transformed under its owner's control. Craft only as a sorcery.)
 *
 * Back face — Mastercraft Raptor (Artifact Creature — Dinosaur, &#42;/4)
 *   Mastercraft Raptor's power is equal to the total power of the exiled cards
 *   used to craft it.
 *
 * Implementation: built from the engine's Craft primitives. The front face's `craft(...)`
 * helper wires the activated ability with a [com.wingedsheep.sdk.scripting.AbilityCost.Craft]
 * material cost paired with the printed mana cost; the resolution effect
 * ([com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect]) returns the
 * source from exile to the battlefield as the back face and re-attaches the
 * `CraftedFromExiledComponent` recording the chosen materials. The back face uses
 * [DynamicAmount.CraftedMaterialsTotalPower] as its power CDA so projection reads the total
 * printed power of those exiled cards each layer pass.
 */

private val DinosaurFilter: GameObjectFilter = GameObjectFilter.Any.withSubtype(Subtype.DINOSAUR)

private val SaheelisLatticeFront = card("Saheeli's Lattice") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you may discard a card. If you do, draw two cards.\n" +
        "Craft with one or more Dinosaurs {4}{R} ({4}{R}, Exile this artifact, Exile one or more Dinosaurs you control and/or Dinosaur cards from your graveyard: Return this card transformed under its owner's control. Craft only as a sorcery.)"

    // ETB: you may discard a card. If you do, draw two cards.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            IfYouDoEffect(
                action = Patterns.Hand.discardCards(1),
                ifYouDo = Effects.DrawCards(2)
            )
        )
    }

    craft(filter = DinosaurFilter, cost = "{4}{R}", materialDescription = "one or more Dinosaurs")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bdc79c6-1193-46bd-931d-2c2a0381e420.jpg?1699044333"
    }
}

private val MastercraftRaptor = card("Mastercraft Raptor") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Artifact Creature — Dinosaur"
    // Power = total power of cards exiled to craft this permanent (CR 702.167c).
    // The CDA reads CraftedFromExiledComponent on this entity each projection pass.
    dynamicPower(DynamicAmount.CraftedMaterialsTotalPower)
    toughness = 4
    oracleText = "Mastercraft Raptor's power is equal to the total power of the exiled cards used to craft it."

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Zoltan Boros"
        flavorText = "While Huatli ventured underground, Saheeli used her unique expertise to bolster the Sun Empire's defenses."
        imageUri = "https://cards.scryfall.io/normal/back/0/b/0bdc79c6-1193-46bd-931d-2c2a0381e420.jpg?1699044333"
    }
}

val SaheelisLattice: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SaheelisLatticeFront,
    backFace = MastercraftRaptor
)
