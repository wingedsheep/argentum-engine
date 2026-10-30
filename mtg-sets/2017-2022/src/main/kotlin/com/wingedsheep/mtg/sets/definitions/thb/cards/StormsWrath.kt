package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Storm's Wrath
 * {2}{R}{R}
 * Sorcery
 * Storm's Wrath deals 4 damage to each creature and each planeswalker.
 *
 * Calamitous Cave-In's sweep at a fixed 4: [Patterns.Group.dealDamageToAll] over creatures and
 * planeswalkers.
 */
val StormsWrath = card("Storm's Wrath") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Storm's Wrath deals 4 damage to each creature and each planeswalker."

    spell {
        effect = Patterns.Group.dealDamageToAll(
            amount = 4,
            filter = GroupFilter(GameObjectFilter.CreatureOrPlaneswalker),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Yeong-Hao Han"
        flavorText = "Distant thunder rumbled, a dread forewarning of Keranos's displeasure."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bc9ecd2-7664-471b-90f2-2d0dd1acec80.jpg?1783931544"
    }
}
