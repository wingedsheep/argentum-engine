package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silver Bolt — Innistrad: Midnight Hunt #258
 * {1} · Artifact · Common
 *
 * {3}, {T}, Sacrifice this artifact: It deals 3 damage to target creature. If a Werewolf is
 * dealt damage this way, destroy it.
 *
 * "Is dealt damage this way" reads what actually happened: the damage instruction stores the
 * damage it really dealt (`damageDealtVariable`, after prevention and redirection, and readable
 * though the artifact was sacrificed as a cost), and the destroy needs both that to be at least 1
 * and the target to be a Werewolf. The Werewolf check is a [Conditions.TargetMatchesFilter] over
 * the same target, read after the damage while the creature is still on the battlefield (no
 * state-based-action pass runs mid-resolution), so it sees projected subtypes.
 */
val SilverBolt = card("Silver Bolt") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T}, Sacrifice this artifact: It deals 3 damage to target creature. " +
        "If a Werewolf is dealt damage this way, destroy it."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val dealt = runStoringNumber { Effects.DealDamage(3, t, damageDealtVariable = it) }
            run(
                Effects.If(
                    condition = Conditions.All(
                        Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GTE, 1),
                        Conditions.TargetMatchesFilter(GameObjectFilter.Any.withSubtype(Subtype.WEREWOLF), t)
                    ),
                    then = Effects.Destroy(t)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "258"
        artist = "Anna Fehr"
        flavorText = "\"When a werewolf charges, you'll only have time for one shot. Best make it " +
            "count.\"\n—Captain Eberheart"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb2c05a8-db56-4397-9b6c-5accb2d3f81f.jpg?1783925546"
    }
}
