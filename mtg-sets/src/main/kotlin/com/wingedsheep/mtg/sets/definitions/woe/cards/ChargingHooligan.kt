package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Charging Hooligan
 * {3}{R}
 * Creature — Human Peasant
 * 3/3
 *
 * Whenever this creature attacks, it gets +1/+0 until end of turn for each attacking creature.
 * If a Rat is attacking, this creature gains trample until end of turn.
 *
 * Both halves are one attack trigger, and both read the board when the ability *resolves*, not
 * when attackers are declared — per the 2023-09-01 ruling the attacker count is taken at
 * resolution and includes Charging Hooligan itself. A blocker killing an attacker in between is
 * impossible (blockers aren't declared yet), but a removal spell cast in response does shrink the
 * bonus, so the count has to stay a resolution-time [DynamicAmount] rather than a baked-in number.
 *
 * The count and the Rat check both scope to [Player.Each] rather than "you control": attacking
 * creatures normally all belong to the attacking player, but a control-change effect resolving
 * mid-combat can leave an attacking Rat under someone else's control, and the printed text asks
 * only whether *a Rat* is attacking.
 */
val ChargingHooligan = card("Charging Hooligan") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Peasant"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks, it gets +1/+0 until end of turn for each " +
        "attacking creature. If a Rat is attacking, this creature gains trample until end of turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            Effects.ModifyStats(
                power = DynamicAmounts.battlefield(
                    Player.Each,
                    GameObjectFilter.Creature.attacking()
                ).count(),
                toughness = DynamicAmount.Fixed(0),
                target = EffectTarget.Self
            ),
            ConditionalEffect(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(
                        Player.Each,
                        GameObjectFilter.Creature.withSubtype(Subtype.RAT).attacking()
                    ).count(),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(1)
                ),
                effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self)
            )
        )
        description = "Whenever this creature attacks, it gets +1/+0 until end of turn for each " +
            "attacking creature. If a Rat is attacking, this creature gains trample until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "318"
        artist = "Tomas Duchek"
        flavorText = "\"Soon the rats are gonna run this town, so I'm gonna run with them!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fddc6f47-202d-4764-abc1-ee453a8917c2.jpg?1783915039"

        ruling(
            "2023-09-01",
            "Count the number of attacking creatures when Charging Hooligan's ability resolves, " +
                "including Charging Hooligan itself, to determine the increase to its power."
        )
    }
}
