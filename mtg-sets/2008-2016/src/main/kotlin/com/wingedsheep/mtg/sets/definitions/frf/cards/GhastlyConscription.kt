package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ghastly Conscription
 * {5}{B}{B}
 * Sorcery
 * Exile all creature cards from target player's graveyard in a face-down pile, shuffle that pile, then
 * manifest those cards.
 *
 * The Jeskai Infiltrator pipeline over a different source: gather the creature cards of the target
 * player's graveyard, exile them face down, `shufflePile` them (each gets a fresh identity, so nobody
 * can tell which manifested 2/2 is which), then manifest the pile. The battlefield move resolves its
 * player to the caster, so cards owned by an opponent are manifested under the caster's control.
 */
val GhastlyConscription = card("Ghastly Conscription") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile all creature cards from target player's graveyard in a face-down pile, shuffle that " +
        "pile, then manifest those cards. (To manifest a card, put it onto the battlefield face down as a 2/2 " +
        "creature. Turn it face up any time for its mana cost if it's a creature card.)"

    spell {
        target(Targets.Player)
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.TargetPlayer, GameObjectFilter.Creature))
            exile(creatures, faceDown = FaceDownMode.HIDDEN)
            val pile = shufflePile(listOf(creatures))
            move(pile, CardDestination.ToZone(Zone.BATTLEFIELD), faceDown = FaceDownMode.MANIFEST)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "70"
        artist = "YW Tang"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/120f7d4a-27b3-43e8-a301-c81d4d403a8f.jpg?1783938696"
        ruling(
            "2014-11-24",
            "The pile is shuffled to disguise from your opponents which manifested creature is which. After you " +
                "manifest the cards, you can look at them."
        )
        ruling(
            "2014-11-24",
            "If you manifest a card owned by an opponent and you leave the game, that card is exiled."
        )
    }
}
