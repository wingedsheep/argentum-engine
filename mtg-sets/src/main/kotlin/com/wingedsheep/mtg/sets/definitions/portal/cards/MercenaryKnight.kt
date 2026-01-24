package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.SacrificeUnlessDiscardEffect

/**
 * Mercenary Knight
 * {2}{B}
 * Creature — Human Mercenary Knight
 * 4/4
 * When Mercenary Knight enters the battlefield, sacrifice it unless you
 * discard a creature card.
 */
val MercenaryKnight = card("Mercenary Knight") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Human Mercenary Knight"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = OnEnterBattlefield()
        effect = SacrificeUnlessDiscardEffect(CardFilter.CreatureCard)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Paolo Parente"
        flavorText = "A mercenary's loyalty lasts only as long as the coin."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a6e09d8-1b84-4c8a-8e35-b7d6a9c3f0a2.jpg"
    }
}
