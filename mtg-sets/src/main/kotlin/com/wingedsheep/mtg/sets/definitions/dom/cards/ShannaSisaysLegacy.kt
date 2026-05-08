package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeTargetedByOpponentAbilities
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Shanna, Sisay's Legacy
 * {G}{W}
 * Legendary Creature — Human Warrior
 * 0/0
 * Shanna, Sisay's Legacy can't be the target of abilities your opponents control.
 * Shanna gets +1/+1 for each creature you control.
 */
val ShannaSisaysLegacy = card("Shanna, Sisay's Legacy") {
    manaCost = "{G}{W}"
    typeLine = "Legendary Creature — Human Warrior"
    power = 0
    toughness = 0
    oracleText = "Shanna, Sisay's Legacy can't be the target of abilities your opponents control.\nShanna gets +1/+1 for each creature you control."

    staticAbility {
        ability = CantBeTargetedByOpponentAbilities
    }

    // +1/+1 for each creature you control (including self — she's 0/0 base)
    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature
            ),
            toughnessBonus = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Magali Villeneuve"
        flavorText = "\"I am heir to many treasures. None is as precious as knowing how my ancestor lived her life.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b90df81c-d738-46b3-8e96-9db0b3507ee0.jpg?1562741797"
    }
}
