package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Esgaroth Garrison
 * {4}{W}
 * Creature — Human Soldier
 * star/5 (power is a characteristic-defining count)
 *
 * Esgaroth Garrison's power is equal to the number of creatures you control.
 * When this creature enters, recruit.
 *
 * The characteristic-defining power is a power-only `dynamicPower(...)` over
 * [DynamicAmount.AggregateBattlefield] counting creatures you control; toughness stays fixed at 5.
 * The Garrison counts itself once it is on the battlefield, so a lone copy is 1/5. Its own recruit
 * trigger resolves after it has entered, so a Soldier token minted that way immediately grows it.
 */
val EsgarothGarrison = card("Esgaroth Garrison") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Esgaroth Garrison's power is equal to the number of creatures you control.\n" +
        "When this creature enters, recruit. (Draw a card, then discard a card. If you discarded " +
        "a nonland card, create a 1/1 white Human Soldier creature token.)"
    toughness = 5

    dynamicPower(
        DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature)
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Mechanic.recruit()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Leonardo Santanna"
        flavorText = "\"I suggest we go to Lake-town,\" said Bilbo. \"What else is there?\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/573f67b0-6ce8-4857-a703-4a5728640736.jpg?1785496934"
    }
}
