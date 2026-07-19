package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAlternativeCastingCost

/**
 * Leyline of Mutation (DSK #188)
 * {2}{G}{G}  Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * You may pay {W}{U}{B}{R}{G} rather than pay the mana cost for spells you cast.
 *
 * The alternative cost is the Jodah, Archmage Eternal ability — [GrantAlternativeCastingCost]
 * with the five-color cost. `mayBeginGameOnBattlefield()` adds the "begin the game on the battlefield" opening-hand
 * marker (CR 103.6). The card is mono-green by its own mana cost but has a five-color identity
 * because of the {W}{U}{B}{R}{G} cost it references.
 */
val LeylineOfMutation = card("Leyline of Mutation") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "You may pay {W}{U}{B}{R}{G} rather than pay the mana cost for spells you cast."

    mayBeginGameOnBattlefield()

    staticAbility {
        ability = GrantAlternativeCastingCost(cost = "{W}{U}{B}{R}{G}")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2359b670-41f0-4ec7-8db9-3f87f7577bc3.jpg?1726286566"
    }
}
