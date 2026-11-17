package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sudden Reclamation
 * {3}{G}
 * Instant
 * Mill four cards, then return a creature card and a land card from your graveyard to your hand.
 *
 * Nothing is targeted: after the mill, the whole graveyard (just-milled cards included) is gathered
 * and you choose a creature card from it (Port of Karfell's shape), then a land card from what's
 * left (Relentless Pursuit's split), so a creature land can fill either slot but never both. Either
 * choice is skipped when the graveyard holds no card of that type; the other still happens.
 */
val SuddenReclamation = card("Sudden Reclamation") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Mill four cards, then return a creature card and a land card from your graveyard to your hand."

    spell {
        effect = Effects.Pipeline {
            mill(4)
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            val (creature, afterCreature) = chooseExactlySplit(
                1,
                from = graveyard,
                filter = GameObjectFilter.Creature,
                prompt = "Choose a creature card to return to your hand",
                selectedLabel = "Return to hand",
            )
            val land = chooseExactly(
                1,
                from = afterCreature,
                filter = GameObjectFilter.Land,
                prompt = "Choose a land card to return to your hand",
                selectedLabel = "Return to hand",
            )
            toHand(creature)
            toHand(land)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Jack Wang"
        flavorText = "\"There is no secret buried so deep that we will never find it.\"\n—Kurtar, Sultai necromancer"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ac7645d-ea10-4ca6-92a5-ac3418c6a510.jpg?1783938678"
        ruling(
            "2014-11-24",
            "You choose which creature card and land card to return to your hand as Sudden Reclamation " +
                "resolves. This doesn't target either card."
        )
        ruling(
            "2014-11-24",
            "If there are no land cards in your graveyard, you still choose a creature card to return, and vice versa."
        )
    }
}
