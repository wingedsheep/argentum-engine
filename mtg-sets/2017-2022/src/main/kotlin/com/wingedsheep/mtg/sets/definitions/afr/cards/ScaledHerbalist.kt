package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Scaled Herbalist
 * {1}{G}
 * Creature — Lizard Druid
 * 1/3
 * {T}: You may put a land card from your hand onto the battlefield.
 *
 * No sorcery-speed restriction, unlike Skyshroud Ranger. [Patterns.Hand.putFromHand] selects
 * *up to* one land card, which is the "you may" — choosing none declines (as on Burgeoning).
 */
val ScaledHerbalist = card("Scaled Herbalist") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard Druid"
    power = 1
    toughness = 3
    oracleText = "{T}: You may put a land card from your hand onto the battlefield."

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
        description = "{T}: You may put a land card from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "April Prime"
        flavorText = "\"I could help you, or I could eat you for lunch. I haven't decided yet.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cb05274-5a94-44e3-889a-667cba1da033.jpg?1783926455"
    }
}
