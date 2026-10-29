package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Selvala's Stampede
 * {4}{G}{G}
 * Sorcery
 *
 * Council's dilemma — Starting with you, each player votes for wild or free. Reveal cards from the
 * top of your library until you reveal a creature card for each wild vote. Put those creature cards
 * onto the battlefield, then shuffle the rest into your library. You may put a permanent card from
 * your hand onto the battlefield for each free vote.
 *
 * The vote hands back one tally per choice; the wild tally is the reveal-until count (zero wild
 * votes reveals nothing) and the free tally caps the "up to" choice from hand. The wild creatures
 * enter together, then the free permanents together, as the rulings order them. The other revealed
 * cards never left the library, so the shuffle alone puts them back.
 */
val SelvalasStampede = card("Selvala's Stampede") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Council's dilemma — Starting with you, each player votes for wild or free. Reveal cards " +
        "from the top of your library until you reveal a creature card for each wild vote. Put those " +
        "creature cards onto the battlefield, then shuffle the rest into your library. You may put a " +
        "permanent card from your hand onto the battlefield for each free vote."

    spell {
        effect = Effects.Pipeline {
            val (wild, free) = vote("wild", "free")
            val (creatures, revealed) = gatherUntilMatch(GameObjectFilter.Creature, count = wild.amount)
            reveal(revealed)
            move(creatures, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(Effects.ShuffleLibrary())
            val handPermanents = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Permanent))
            val chosen = chooseUpTo(
                free.amount,
                from = handPermanents,
                prompt = "Put a permanent card from your hand onto the battlefield for each free vote"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85d6e48e-3d26-4915-9739-25edbd80bdf1.jpg?1783937340"
        ruling("2016-08-23", "You must vote for one of the available options. You can't abstain.")
        ruling("2016-08-23", "The creature cards revealed for the wild votes all enter the battlefield at the same time, followed by the permanent cards put onto the battlefield from your hand for the free votes.")
        ruling("2016-08-23", "Because the votes are made in turn order, each player will know the votes of players who voted beforehand.")
    }
}
