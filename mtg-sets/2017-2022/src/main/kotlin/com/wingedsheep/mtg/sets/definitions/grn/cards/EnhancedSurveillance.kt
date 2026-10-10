package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Enhanced Surveillance
 * {1}{U}
 * Enchantment
 *
 * You may look at an additional two cards each time you surveil.
 * Exile this enchantment: Shuffle your graveyard into your library.
 *
 * Notes:
 *  - The first ability is [ModifyKeywordActionAmount], applied at the surveil announcement: the extra
 *    cards are part of what you surveil (CR 701.25b), so each may go to the graveyard or back on
 *    top, and "whenever you surveil" payoffs see the bigger look. Two copies stack (+4).
 *  - The "may" isn't modelled: looking at the extra two is a superset of not looking, since every
 *    one of them can go back on top in any order.
 */
val EnhancedSurveillance = card("Enhanced Surveillance") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "You may look at an additional two cards each time you surveil.\n" +
        "Exile this enchantment: Shuffle your graveyard into your library."

    // You may look at an additional two cards each time you surveil.
    replacementEffect(ModifyKeywordActionAmount(EventPattern.SurveilEvent(), modifier = 2))

    // Exile this enchantment: Shuffle your graveyard into your library.
    activatedAbility {
        cost = Costs.ExileSelf
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/971d254e-da25-494b-a16d-3d7d6bb75c73.jpg?1783934188"
        ruling("2018-10-05", "The additional cards you look at due to Enhanced Surveillance's ability are part of what you surveil. You may put those cards into your graveyard or back on top in any order along with the others.")
        ruling("2018-10-05", "If you control a second Enhanced Surveillance, their effects both apply and you may look at an additional four cards. If you control a third, may look at an additional six cards, and so on.")
        ruling("2018-10-05", "Enhanced Surveillance's last ability can be activated only while it's on the battlefield.")
    }
}
