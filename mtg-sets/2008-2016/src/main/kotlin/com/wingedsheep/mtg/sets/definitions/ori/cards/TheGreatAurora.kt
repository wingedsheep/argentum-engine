package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Great Aurora — Magic Origins #179
 * {6}{G}{G}{G} · Sorcery
 *
 * Each player shuffles all cards from their hand and all permanents they own into their library,
 * then draws that many cards. Each player may put any number of land cards from their hand onto
 * the battlefield. Exile The Great Aurora.
 *
 * Every step happens for all players at once, so each is one step over everyone's cards:
 *
 * 1. Every hand and every permanent (each permanent is owned by some player) leaves in one move,
 *    each card into its *owner's* library (CR 400.3) — a permanent you control but don't own goes
 *    to its owner, and counts for them. Then each player shuffles, including a player who put
 *    nothing in (the Timetwister ruling). Tokens are shuffled in too and count toward "that many"
 *    (the ruling), but they are no cards to draw: a token that left the battlefield can't change
 *    zones again (CR 111.8), so the draw passes over it, and a library left with only tokens is
 *    empty — its player fails to draw and loses at the next state-based action check (CR 704.5b).
 *    The tokens themselves cease to exist at that same check (CR 704.5d).
 * 2. "That many" is per player: whatever is now in a library, tallied by owner. Off the battlefield
 *    a card has no controller, so `captureControllers` reads each card's owner here, and
 *    `forEachCaptured` has each owner draw their own tally in turn order. A card a replacement
 *    sent somewhere else wasn't shuffled in, so it isn't counted.
 * 3. Each player, in turn order, chooses any number of land cards from their hand (CR 101.4, the
 *    ruling); nothing moves until everyone has chosen, then all the lands enter at once under their
 *    owners' control.
 * 4. `selfExile()` — the spell goes to exile instead of its owner's graveyard.
 */
val TheGreatAurora = card("The Great Aurora") {
    manaCost = "{6}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Each player shuffles all cards from their hand and all permanents they own into their " +
        "library, then draws that many cards. Each player may put any number of land cards from their " +
        "hand onto the battlefield. Exile The Great Aurora."

    spell {
        selfExile()
        effect = Effects.Pipeline {
            val gone = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.HAND, Zone.BATTLEFIELD),
                    player = Player.Each,
                )
            )
            move(gone, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom))
            run(Effects.ForEachPlayer(Player.Each, Effects.ShuffleLibrary()))

            val shuffledIn = filter(gone, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
            val owners = captureControllers(shuffledIn)
            forEachCaptured(shuffledIn, original = shuffledIn, controllers = owners) { thatMany ->
                run(Effects.DrawCards(thatMany.amount))
            }

            val (lands) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val handLands = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Land))
                listOf(
                    chooseAnyNumber(
                        handLands,
                        prompt = "Choose any number of land cards to put onto the battlefield",
                        selectedLabel = "Put onto the battlefield",
                    )
                )
            }
            move(lands, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "179"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1d0eb3b-00b9-42ca-9f7d-5543bffc3480.jpg?1783938322"
        ruling(
            "2015-06-22",
            "As The Great Aurora resolves, first you choose any number of land cards in your hand to put " +
                "onto the battlefield, and then each other player in turn order does the same. The lands " +
                "enter the battlefield simultaneously.",
        )
        ruling(
            "2015-06-22",
            "The number of cards you draw is equal to the number of permanents on the battlefield that you " +
                "shuffle into your library plus the number of cards from your hand that you shuffle into " +
                "your library.",
        )
        ruling(
            "2015-06-22",
            "A token is owned by the player under whose control it entered the battlefield. Tokens shuffled " +
                "into a library count toward the number of cards drawn, though they cease to exist upon " +
                "becoming part of a library.",
        )
    }
}
