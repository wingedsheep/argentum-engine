package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Helping Hand — {W}
 * Sorcery
 * Uncommon — The Lost Caverns of Ixalan #17
 * Artist: Aldo Domínguez
 *
 * "Return target creature card with mana value 3 or less from your graveyard to the battlefield tapped."
 *
 * The target must be a creature card in the controller's own graveyard with mana value ≤ 3
 * (CardPredicate.ManaValueAtMost(3) + ControllerPredicate.OwnedByYou in Zone.GRAVEYARD).
 * The card enters the battlefield tapped (PutOntoBattlefield with tapped = true).
 */
val HelpingHand = card("Helping Hand") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target creature card with mana value 3 or less from your graveyard to the battlefield tapped."

    spell {
        val t = target(
            "target creature card with mana value 3 or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter(
                        cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.ManaValueAtMost(3)),
                        controllerPredicate = ControllerPredicate.OwnedByYou
                    ),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.PutOntoBattlefield(t, tapped = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Aldo Domínguez"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8aa7126-5df3-42dd-b4a3-0d0ea59eeebd.jpg?1782694598"
    }
}
