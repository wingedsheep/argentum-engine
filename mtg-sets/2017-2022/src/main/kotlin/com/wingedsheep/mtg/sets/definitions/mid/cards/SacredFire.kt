package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Sacred Fire
 * {R}{W}
 * Instant
 * Sacred Fire deals 2 damage to any target and you gain 2 life.
 * Flashback {4}{R}{W}
 */
val SacredFire = card("Sacred Fire") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Sacred Fire deals 2 damage to any target and you gain 2 life.\n" +
        "Flashback {4}{R}{W} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t) then Effects.GainLife(2)
    }

    keywordAbility(KeywordAbility.flashback("{4}{R}{W}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Svetlin Velinov"
        flavorText = "\"Do not confuse justice with mercy.\"\n—Odric, Order of Saint Traft"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0b1dcd7-6dd9-4134-bc6c-9dc7754006a2.jpg?1783925554"
    }
}
