package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn

/**
 * Rule of Law
 * {2}{W}
 * Enchantment
 * Each player can't cast more than one spell each turn.
 *
 * The global (`eachPlayer = true`) form of [RestrictSpellsCastPerTurn] — the same primitive
 * High Noon uses. The engine compares each player's spells-cast-this-turn tally against the
 * cap during cast-legality checks, so spells cast earlier in the turn count even if Rule of
 * Law entered afterwards, and a countered spell still counts (both Scryfall rulings below).
 */
val RuleOfLaw = card("Rule of Law") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Each player can't cast more than one spell each turn."

    staticAbility {
        ability = RestrictSpellsCastPerTurn(maxPerTurn = 1, eachPlayer = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Scott M. Fischer"
        flavorText = "Appointed by the kha himself, members of the tribunal ensure all " +
            "disputes are settled with the utmost fairness."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/246a68e9-fd19-4a1e-8c7b-fcd3f7306dfb.jpg?1783944559"
        ruling(
            "2019-07-12",
            "Rule of Law looks at the entire turn to see if a player has cast a spell, even " +
                "if Rule of Law wasn't on the battlefield when that spell was cast. Notably, " +
                "you can't cast Rule of Law and then cast another spell during the same turn."
        )
        ruling(
            "2019-07-12",
            "If you cast a spell that was countered, you can't cast another spell during the same turn."
        )
    }
}
