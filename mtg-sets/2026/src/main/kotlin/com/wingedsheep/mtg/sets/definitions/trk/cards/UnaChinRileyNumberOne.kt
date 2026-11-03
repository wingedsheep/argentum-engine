package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Una Chin-Riley, Number One — Star Trek #42
 * {4}{W} · Legendary Creature — Illyrian Officer · 4/5
 *
 * Whenever you attack while creatures you control have total power 8 or greater, target
 * attacking creature gets +2/+0 and gains first strike until end of turn.
 *
 * Same trigger shape as Tactical Officer: the "while …" total-power gate is a
 * `triggerRestriction` checked as attackers are declared; the payoff is the Most Valuable
 * Slayer target-an-attacker buff.
 */
val UnaChinRileyNumberOne = card("Una Chin-Riley, Number One") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Illyrian Officer"
    oracleText = "Whenever you attack while creatures you control have total power 8 or greater, " +
        "target attacking creature gets +2/+0 and gains first strike until end of turn."
    power = 4
    toughness = 5

    triggeredAbility {
        trigger = Triggers.you.attacks()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        val attacker = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(2, 0, attacker) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, attacker)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Josu Hernaiz"
        flavorText = "\"Starfleet is not a perfect organization, but it strives to be. And I " +
            "believe it could be.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5585c90d-3fe7-41f4-9728-36b9410ecc53.jpg?1792668590"
    }
}
