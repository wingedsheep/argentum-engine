package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wojek Investigator — Murders at Karlov Manor #36
 * {2}{W} · Creature — Angel Detective · 2/4 · Rare
 *
 * Flying, vigilance
 * At the beginning of your upkeep, investigate once for each opponent who has more cards in hand
 * than you.
 *
 * A catch-up engine: it pays you only while you are behind on cards, and it pays *per* opponent,
 * so in a pod it can hand you several Clues in one upkeep. Strictly "more than", so an opponent
 * tied with you contributes nothing.
 *
 * The count is [DynamicAmount.CountPlayersWith] over [Player.EachOpponent], and the comparison
 * inside it is where the subtlety lives. That loop re-evaluates its condition once per candidate
 * with the context's controller **rebound to that candidate**, so `Player.You` inside means "the
 * opponent being tested" — which is what we want on the left of the comparison, but leaves the
 * ability's own controller unreachable on the right. [Player.ControllerOfSource] is that missing
 * reference: it reads control off the Investigator itself rather than off the rebound context, so
 * the comparison stays "that opponent's hand > *your* hand" for every candidate.
 *
 * Hand sizes are read when the ability **resolves**, not when it triggers — an opponent who
 * discards in response drops out of the count, and one who draws in response joins it.
 */
val WojekInvestigator = card("Wojek Investigator") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Detective"
    power = 2
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "At the beginning of your upkeep, investigate once for each opponent who has more cards " +
        "in hand than you. (To investigate, create a Clue token. It's an artifact with \"{2}, " +
        "Sacrifice this token: Draw a card.\")"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Investigate(
            DynamicAmount.CountPlayersWith(
                scope = Player.EachOpponent,
                condition = Conditions.CompareAmounts(
                    left = DynamicAmount.Count(Player.You, Zone.HAND),
                    operator = ComparisonOperator.GT,
                    right = DynamicAmount.Count(Player.ControllerOfSource, Zone.HAND),
                ),
            )
        )
        description = "At the beginning of your upkeep, investigate once for each opponent who " +
            "has more cards in hand than you."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Ben Hill"
        flavorText = "\"Well, this clearly didn't come from anyone I know...\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/296574c6-3933-4ab3-b591-72514b244da9.jpg?1783912917"
    }
}
