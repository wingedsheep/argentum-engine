package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Burning Oil
 * {1}{R}
 * Instant
 * Burning Oil deals 3 damage to target attacking or blocking creature.
 * Flashback {3}{W}
 */
val BurningOil = card("Burning Oil") {
    manaCost = "{1}{R}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Burning Oil deals 3 damage to target attacking or blocking creature.\n" +
        "Flashback {3}{W} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val t = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.DealDamage(3, t)
    }
    keywordAbility(KeywordAbility.flashback("{3}{W}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Trevor Claxton"
        flavorText = "Every now and then, a devil's prank can give you a good idea."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47773da8-afe4-43e1-8355-6ab51451ee00.jpg"
    }
}
