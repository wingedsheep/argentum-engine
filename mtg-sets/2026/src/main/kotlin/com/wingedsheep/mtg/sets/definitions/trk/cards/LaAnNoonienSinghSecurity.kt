package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * La'An Noonien-Singh, Security — Star Trek #156
 * {2}{R}{R} · Legendary Creature — Human Soldier · 4/3
 *
 * Whenever you attack while creatures you control have total power 8 or greater, target
 * creature can't block this turn.
 *
 * "Whenever you attack" fires once per combat on the attack declaration; "while …" gates the
 * trigger itself, so the total-power check is a `triggerRestriction` made as attackers are
 * declared, not an intervening-if rechecked on resolution (same shape as Tactical Officer).
 */
val LaAnNoonienSinghSecurity = card("La'An Noonien-Singh, Security") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier"
    oracleText = "Whenever you attack while creatures you control have total power 8 or greater, " +
        "target creature can't block this turn."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.attacks()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        val t = target(TargetFilter.Creature)
        effect = Effects.CantBlock(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Pauline Voss"
        flavorText = "\"Lesson six of security: know when to bend the rules.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afe08bef-b184-4b91-8d69-0101e183c3ff.jpg?1785981540"
    }
}
