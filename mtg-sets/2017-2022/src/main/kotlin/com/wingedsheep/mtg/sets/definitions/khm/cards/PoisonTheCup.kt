package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Poison the Cup
 * {1}{B}{B}
 * Instant
 * Destroy target creature. If this spell was foretold, scry 2.
 * Foretell {1}{B} (During your turn, you may pay {2} and exile this card from your hand face down. Cast it on a later turn for its foretell cost.)
 *
 * The scry rider reads [Conditions.WasForetold] — true when the spell was a foretold card in exile
 * before it was cast (CR 702.143c), so a cast from hand only destroys.
 */
val PoisonTheCup = card("Poison the Cup") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature. If this spell was foretold, scry 2.\n" +
        "Foretell {1}{B} (During your turn, you may pay {2} and exile this card from your hand " +
        "face down. Cast it on a later turn for its foretell cost.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Destroy(t) then Effects.If(Conditions.WasForetold, Effects.Scry(2))
    }

    keywordAbility(KeywordAbility.foretell("{1}{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Colin Boyer"
        flavorText = "Dark schemes cut down what armies cannot."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fb94456-5266-47db-b514-a0e17e34b771.jpg?1783928243"
    }
}
