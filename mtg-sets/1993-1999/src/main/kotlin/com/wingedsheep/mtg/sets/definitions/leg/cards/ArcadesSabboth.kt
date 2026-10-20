package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arcades Sabboth
 * {2}{G}{G}{W}{W}{U}{U}
 * Legendary Creature — Elder Dragon
 * 7/7
 *
 * Flying
 * At the beginning of your upkeep, sacrifice Arcades Sabboth unless you pay {G}{W}{U}.
 * Each untapped creature you control gets +0/+2 as long as it's not attacking.
 * {W}: Arcades Sabboth gets +0/+1 until end of turn.
 *
 * The defensive aura is a static [ModifyStats] over "untapped, nonattacking creatures you
 * control" — a vigilant attacker stays untapped but loses the bonus while it's attacking.
 */
val ArcadesSabboth = card("Arcades Sabboth") {
    manaCost = "{2}{G}{G}{W}{W}{U}{U}"
    colorIdentity = "GUW"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 7
    toughness = 7
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, sacrifice Arcades Sabboth unless you pay {G}{W}{U}.\n" +
        "Each untapped creature you control gets +0/+2 as long as it's not attacking.\n" +
        "{W}: Arcades Sabboth gets +0/+1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(cost = Costs.pay.Mana("{G}{W}{U}"), suffer = SacrificeSelfEffect)
    }

    staticAbility {
        ability = ModifyStats(
            0, 2,
            GroupFilter(
                GameObjectFilter.Creature.untapped().youControl()
                    .withStatePredicate(StatePredicate.Not(StatePredicate.IsAttacking))
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.ModifyStats(0, 1, EffectTarget.Self)
        description = "{W}: Arcades Sabboth gets +0/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c1dbc62-ceb5-4540-ae38-901e5deafc75.jpg?1783948041"
    }
}
