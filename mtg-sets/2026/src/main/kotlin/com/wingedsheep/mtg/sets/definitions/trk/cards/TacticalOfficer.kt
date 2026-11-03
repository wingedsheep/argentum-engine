package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tactical Officer — Star Trek #38
 * {1}{W} · Creature — Human Officer · 2/1
 *
 * Whenever you attack while creatures you control have total power 8 or greater, put a +1/+1
 * counter on this creature. Untap it.
 *
 * "Whenever you attack" fires once per combat on the attack declaration; "while …" gates the
 * trigger itself, so the total-power check is a `triggerRestriction` made as attackers are
 * declared, not an intervening-if rechecked on resolution (same shape as Kirk, Enterprising
 * Captain).
 */
val TacticalOfficer = card("Tactical Officer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Officer"
    oracleText = "Whenever you attack while creatures you control have total power 8 or greater, " +
        "put a +1/+1 counter on this creature. Untap it."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.attacks()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Elizabeth Peiró"
        flavorText = "\"Kouassi to bridge. Captain, the intruders look like Starfleet, but their " +
            "combadge is different. It's . . . a dagger through a planet?\""
        imageUri = "https://cards.scryfall.io/normal/front/3/1/317704a8-a3c2-400c-bbe0-2cbc6fe10710.jpg?1785981383"
    }
}
