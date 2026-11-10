package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gravity Negator
 * {3}{U}
 * Creature — Eldrazi Drone
 * 2/3
 * Devoid (This card has no color.)
 * Flying
 * Whenever this creature attacks, you may pay {C}. If you do, another target creature gains flying
 * until end of turn. ({C} represents colorless mana.)
 */
val GravityNegator = card("Gravity Negator") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "Whenever this creature attacks, you may pay {C}. If you do, another target creature gains flying " +
        "until end of turn. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val other = target(TargetFilter.OtherCreature)
        effect = Effects.MayPay(ManaCost.parse("{C}"), Effects.GrantKeyword(Keyword.FLYING, other))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85f9b63e-6ecc-4d85-9724-87b8176ae6a6.jpg"
    }
}
