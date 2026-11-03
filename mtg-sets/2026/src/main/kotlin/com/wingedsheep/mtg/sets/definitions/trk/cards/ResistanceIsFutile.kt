package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Resistance Is Futile
 * {1}{B}
 * Sorcery
 *
 * Choose one —
 * • Destroy target nonartifact creature.
 * • Return target artifact creature card with mana value 3 or less from your graveyard to the
 *   battlefield.
 */
val ResistanceIsFutile = card("Resistance Is Futile") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Destroy target nonartifact creature.\n" +
        "• Return target artifact creature card with mana value 3 or less from your graveyard to the battlefield."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target nonartifact creature") {
                val creature = target(TargetFilter.Creature.nonartifact())
                effect = Effects.Destroy(creature)
            }

            mode("Return target artifact creature card with mana value 3 or less from your graveyard to the battlefield") {
                val card = target(
                    TargetFilter(
                        GameObjectFilter.ArtifactCreature.manaValueAtMost(3).ownedByYou(),
                        zone = Zone.GRAVEYARD
                    )
                )
                effect = Effects.PutOntoBattlefieldFromGraveyard(card)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81d88257-466d-4729-862f-591885b3d875.jpg?1785981518"
    }
}
