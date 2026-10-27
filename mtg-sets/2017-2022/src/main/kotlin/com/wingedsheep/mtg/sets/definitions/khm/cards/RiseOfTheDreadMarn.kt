package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rise of the Dread Marn — Kaldheim #107
 * {2}{B} · Instant
 *
 * Create X 2/2 black Zombie Berserker creature tokens, where X is the number of nontoken
 * creatures that died this turn.
 * Foretell {B}
 *
 * X is evaluated as the spell resolves (so nontoken creatures that die in response count) and
 * sums every seat's nontoken-creatures-died tracker ([Player.Each]) — the printed text counts
 * creatures no matter who controlled them, as Gadrak, the Crown-Scourge does. Foretell is lowered
 * into [KeywordAbility.foretell], which drives the pay-{2}-and-exile special action and the later
 * {B} cast from exile.
 */
val RiseOfTheDreadMarn = card("Rise of the Dread Marn") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Create X 2/2 black Zombie Berserker creature tokens, where X is the number of " +
        "nontoken creatures that died this turn.\n" +
        "Foretell {B} (During your turn, you may pay {2} and exile this card from your hand face " +
        "down. Cast it on a later turn for its foretell cost.)"

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.nonTokenCreaturesDiedThisTurn(Player.Each),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Berserker"),
        )
    }

    keywordAbility(KeywordAbility.foretell("{B}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d3a1ec1-f362-494d-a23b-6a963ce44fdd.jpg?1783928240"
        ruling(
            "2021-02-05",
            "The number of nontoken creatures that died that turn is counted as Rise of the Dread Marn " +
                "is resolving. In other words, nontoken creatures that die in response to Rise of the " +
                "Dread Marn will count."
        )
        ruling(
            "2021-02-05",
            "It doesn't matter if the cards that represented those creatures are still in the graveyard " +
                "as Rise of the Dread Marn resolves. They'll still count as long as they died that turn."
        )
    }
}
