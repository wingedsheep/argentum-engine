package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Aggressive Sabotage
 * {2}{B}
 * Sorcery
 *
 * Kicker {R}
 * Target player discards two cards. If this spell was kicked, it deals 3 damage to that player.
 */
val AggressiveSabotage = card("Aggressive Sabotage") {
    manaCost = "{2}{B}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Kicker {R} (You may pay an additional {R} as you cast this spell.)\nTarget player discards two cards. If this spell was kicked, it deals 3 damage to that player."

    keywordAbility(KeywordAbility.kicker("{R}"))

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Hand.discardCards(2, player) then
            Effects.If(Conditions.WasKicked, Effects.DealDamage(3, player))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Daarken"
        flavorText = "Squee's orders basically came down to \"attack the squishy parts.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac809fca-664a-4e7d-a39f-2bc1dea0fbd8.jpg?1783921339"
    }
}
