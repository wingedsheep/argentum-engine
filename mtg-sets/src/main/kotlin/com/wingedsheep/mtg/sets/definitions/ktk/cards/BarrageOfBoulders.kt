package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Barrage of Boulders
 * {2}{R}
 * Sorcery
 * Barrage of Boulders deals 1 damage to each creature you don't control.
 * Ferocious — If you control a creature with power 4 or greater, creatures can't block this turn.
 */
val BarrageOfBoulders = card("Barrage of Boulders") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Barrage of Boulders deals 1 damage to each creature you don't control.\nFerocious — If you control a creature with power 4 or greater, creatures can't block this turn."

    spell {
        effect = GroupPatterns.dealDamageToAll(1, GroupFilter.AllCreaturesOpponentsControl)
            .then(ConditionalEffect(
                condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
                effect = Effects.CantBlockGroup(GroupFilter.AllCreatures)
            ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Zoltan Boros"
        flavorText = "\"Crude tactics can be effective nonetheless.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2eb1a9f7-32ba-48fd-a7f7-788b0ec052c6.jpg?1562784418"
    }
}
