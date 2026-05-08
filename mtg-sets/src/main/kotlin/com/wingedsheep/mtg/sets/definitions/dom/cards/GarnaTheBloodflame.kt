package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Garna, the Bloodflame
 * {3}{B}{R}
 * Legendary Creature — Human Warrior
 * 3/3
 * Flash
 * When Garna enters, return to your hand all creature cards in your graveyard
 * that were put there from anywhere this turn.
 * Other creatures you control have haste.
 */
val GarnaTheBloodflame = card("Garna, the Bloodflame") {
    manaCost = "{3}{B}{R}"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Flash\nWhen Garna, the Bloodflame enters the battlefield, return to your hand all creature cards in your graveyard that were put there from anywhere this turn.\nOther creatures you control have haste."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ReturnCreaturesPutInGraveyardThisTurn()
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl()).other()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae9c1471-ae6f-4b66-9842-46f1a74e93b5.jpg?1562741280"
    }
}
