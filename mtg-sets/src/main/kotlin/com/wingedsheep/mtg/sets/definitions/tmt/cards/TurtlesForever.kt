package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Turtles Forever
 * {3}{W}
 * Instant
 *
 * Search your library and/or outside the game for exactly four legendary creature cards
 * you own with different names, then reveal those cards. An opponent chooses two of them.
 * Put the chosen cards into your hand and shuffle the rest into your library.
 *
 * A two-chooser "you assemble, they split" pile, composed from the atomic pipeline facade
 * ([Effects.Pipeline]):
 *  1. Gather every legendary creature card across your library *and* "outside the game" — the
 *     private [Zone.SIDEBOARD] (CR 100.4 / 400.11a), as the wish cycle uses.
 *  2. You search: `chooseExactly(4)` with [SelectionRestriction.OnePerCardName] for "exactly
 *     four ... with different names" (clamps down if fewer eligible exist).
 *  3. Reveal the four you found.
 *  4. The *opponent* ([Chooser.Opponent]) splits them via `chooseExactlySplit(2)`: two chosen go
 *     to your hand; the remainder is moved into your library and shuffled (note: unchosen cards
 *     pulled from outside the game end up in your library, not back in the sideboard, exactly as
 *     printed). The remainder move keeps the default placement — the trailing shuffle makes its
 *     order moot, so there's deliberately no library-ordering prompt.
 */
val TurtlesForever = card("Turtles Forever") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Search your library and/or outside the game for exactly four legendary creature cards " +
        "you own with different names, then reveal those cards. An opponent chooses two of them. " +
        "Put the chosen cards into your hand and shuffle the rest into your library."

    spell {
        effect = Effects.Pipeline {
            val pool = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.LIBRARY, Zone.SIDEBOARD),
                    player = Player.You,
                    filter = GameObjectFilter.Creature.legendary(),
                ),
            )
            val found = chooseExactly(
                4, from = pool,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for exactly four legendary creature cards with different names",
            )
            reveal(found)
            val (chosen, rest) = chooseExactlySplit(
                2, from = found, chooser = Chooser.Opponent, alwaysPrompt = true,
                prompt = "Choose two of those cards to put into their hand",
            )
            toHand(chosen)
            move(rest, CardDestination.ToZone(Zone.LIBRARY, Player.You))
            run(ShuffleLibraryEffect())
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Devin Elle Kurtz"
        flavorText = "\"See you around the multiverse, bros.\"\n—One Leonardo or another"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0db974a-3289-4727-9aaf-e9cca9113c87.jpg?1760102536"
    }
}
