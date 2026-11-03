package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Starfleet Security
 * {R}
 * Creature — Human Officer
 * 1/1
 * Haste
 * When this creature dies during your turn, investigate.
 *
 * "During your turn" narrows when the trigger fires (CR 603.2), so it is a
 * triggerRestriction checked as the creature dies — not an intervening-if.
 */
val StarfleetSecurity = card("Starfleet Security") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Officer"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "When this creature dies during your turn, investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        triggerRestriction = Conditions.IsYourTurn
        effect = Effects.Investigate()
        description = "When this creature dies during your turn, investigate."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Edouard"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62acde0e-791d-4259-83bc-3690e498f3b0.jpg?1793359516"
    }
}
