package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Melded Moxite
 * {1}{R}
 * Artifact
 * When this artifact enters, you may discard a card. If you do, draw two cards.
 * {3}, Sacrifice this artifact: Create a tapped 2/2 colorless Robot artifact creature token.
 */
val MeldedMoxite = card("Melded Moxite") {
    manaCost = "{1}{R}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you may discard a card. If you do, draw two cards.\n{3}, Sacrifice this artifact: Create a tapped 2/2 colorless Robot artifact creature token."

    // ETB ability: May discard a card to draw two cards
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            EffectPatterns.discardCards(1).then(Effects.DrawCards(2))
        )
    }

    // Activated ability: {3}, Sacrifice this artifact: Create a tapped 2/2 Robot token
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.SacrificeSelf
        )
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(), // colorless
            creatureTypes = setOf("Robot"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Alexandr Leskinen"
        flavorText = "Only the most skilled matter shapers can imbue moxite with other raw energies, but the rewards are well worth the dangers."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/474c067f-eb24-4ae8-b4c0-f6f8e24cdb2a.jpg?1752947131"
    }
}
