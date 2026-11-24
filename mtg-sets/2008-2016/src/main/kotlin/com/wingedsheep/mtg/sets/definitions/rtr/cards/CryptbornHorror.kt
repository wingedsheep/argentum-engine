package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cryptborn Horror — Return to Ravnica #212 (canonical printing)
 * {1}{B/R}{B/R} · Creature — Horror · 0/0
 *
 * Trample
 * This creature enters with X +1/+1 counters on it, where X is the total life lost by your
 * opponents this turn.
 *
 * An [EntersWithDynamicCounters] replacement (CR 614.1c) summing every opponent's life-lost-this-turn
 * tally — the total lost, not the net change in life total (see rulings). With no life lost it enters
 * as a 0/0 and dies to state-based actions.
 */
val CryptbornHorror = card("Cryptborn Horror") {
    manaCost = "{1}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Creature — Horror"
    power = 0
    toughness = 0
    oracleText = "Trample\nThis creature enters with X +1/+1 counters on it, where X is the total life lost by your opponents this turn."

    keywords(Keyword.TRAMPLE)

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.lifeLostThisTurn(Player.EachOpponent)))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Richard Wright"
        flavorText = "Although Cryptwitch Merree vanished after a spat with Rakdos, her playthings lingered."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8ffd77d7-3fe6-4493-96eb-f62183c0358d.jpg?1783940327"
        ruling("2012-10-01", "If no opponent has lost life on the turn Cryptborn Horror enters, it will enter with no +1/+1 counters on it and be put into its owner's graveyard (unless something else is raising its toughness).")
        ruling("2012-10-01", "Cryptborn Horror cares about the total life lost, not necessarily what an opponent's life total is compared to what it was at the beginning of the turn. For example, if an opponent loses 5 life and then gains 10 life on the turn Cryptborn Horror enters, it will enter with five +1/+1 counters on it.")
        ruling("2012-10-01", "In multiplayer games, life lost the turn Cryptborn Horror enters by opponents who left the game later that turn will count toward the value of X.")
    }
}
