package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Survivor of Korlis
 * {W}
 * Creature — Human Soldier
 * 1/1
 * First strike
 * {1}{W}, Exile this card from your graveyard: Scry 2.
 */
val SurvivorOfKorlis = card("Survivor of Korlis") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "First strike\n" +
        "{1}{W}, Exile this card from your graveyard: Scry 2."

    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.ExileSelf)
        effect = Effects.Scry(2)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Julia Metzger"
        flavorText = "At first, the merchant state of Korlis saw no need to involve itself in the conflict between Yotia and the Fallaji. Mishra's dragon engines changed that."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/817bcc8d-a5b7-448c-a3eb-825dc65944ec.jpg"
    }
}
