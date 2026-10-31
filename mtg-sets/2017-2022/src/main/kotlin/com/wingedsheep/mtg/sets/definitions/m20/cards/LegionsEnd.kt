package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Legion's End — Core Set 2020 #106
 * {1}{B} · Sorcery
 *
 * Exile target creature an opponent controls with mana value 2 or less and all other creatures
 * that player controls with the same name as that creature. Then that player reveals their hand
 * and exiles all cards with that name from their hand and graveyard.
 *
 * Sever the Bloodline's name pipeline, scoped to the target's controller. Every collection is
 * gathered while the target is still on the battlefield; the creatures are exiled first, then the
 * gathered hand is revealed and the same-named cards from hand and graveyard are exiled into
 * "that player"'s exile (read off the target's last-known controller).
 */
val LegionsEnd = card("Legion's End") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile target creature an opponent controls with mana value 2 or less and all other " +
        "creatures that player controls with the same name as that creature. Then that player reveals " +
        "their hand and exiles all cards with that name from their hand and graveyard."

    spell {
        target(TargetFilter.Creature.manaValueAtMost(2).opponentControls())
        val thatPlayer = Player.ControllerOf("target creature")
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val chosenName = storeCardName(chosen)
            val creatures = gather(
                CardSource.ControlledPermanents(
                    player = thatPlayer,
                    filter = GameObjectFilter.Creature.namedFromVariable(chosenName),
                )
            )
            val hand = gather(CardSource.FromZone(Zone.HAND, player = thatPlayer))
            val graveyardMatches = gather(
                CardSource.FromZone(
                    Zone.GRAVEYARD,
                    player = thatPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName),
                )
            )
            exile(creatures)
            reveal(hand)
            val handMatches = filter(hand, GameObjectFilter.Any.namedFromVariable(chosenName))
            exile(handMatches, owner = thatPlayer)
            exile(graveyardMatches, owner = thatPlayer)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "David Palumbo"
        flavorText = "\"With a single word, they were unmade.\"\n—Krinnea, *Siege of the Bone Spire*"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49a1cd92-9d75-4e22-a934-a26d84967015.jpg?1783932991"
    }
}
