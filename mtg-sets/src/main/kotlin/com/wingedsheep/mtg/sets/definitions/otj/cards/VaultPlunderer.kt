package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
/**
 * Vault Plunderer
 * {2}{B}
 * Creature — Human Rogue
 * 3/1
 * When this creature enters, target player draws a card and loses 1 life.
 */
val VaultPlunderer = card("Vault Plunderer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, target player draws a card and loses 1 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", Targets.Player)
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(1, t),
                Effects.LoseLife(1, t),
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Evyn Fong"
        flavorText = "\"Kind of them to put all their valuables in the same place.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e6bf35c-8763-47cc-ab2d-5dbabeb28072.jpg?1712355713"
    }
}
