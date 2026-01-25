package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OnEnterBattlefield

/**
 * Owl Familiar
 * {1}{U}
 * Creature - Bird
 * 1/1
 * Flying
 * When Owl Familiar enters, draw a card, then discard a card.
 */
val OwlFamiliar = card("Owl Familiar") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = OnEnterBattlefield()
        effect = Effects.Loot()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Janine Johnston"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8a7e16e-a5f8-4c47-b6d8-e3d8bd8db46f.jpg"
    }
}
