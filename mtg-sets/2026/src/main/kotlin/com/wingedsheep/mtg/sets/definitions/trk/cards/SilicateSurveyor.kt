package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.model.Rarity

/**
 * Silicate Surveyor
 * {4}{R}
 * Creature — Tholian Scout
 * 4/4
 *
 * Reach
 * When this creature enters, exile the top card of your library. Until the end of your next turn, you may play that card.
 */
val SilicateSurveyor = card("Silicate Surveyor") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Tholian Scout"
    colorIdentity = "R"
    oracleText = "Reach\nWhen this creature enters, exile the top card of your library. Until the end of your next turn, you may play that card."
    power = 4
    toughness = 4
    keywords(Keyword.REACH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Exile.impulse(1, MayPlayExpiry.UntilEndOfNextTurn)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Joan Piqué Llorens"
        flavorText = "The Tholian Assembly does not suffer trespassers lightly."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6359ab5d-4151-4a0c-af13-d59c1b381bb0.jpg?1786005521"
    }
}
