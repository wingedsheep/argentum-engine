package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker

/**
 * Avatar Aang // Aang, Master of Elements — {R}{G}{W}{U} Legendary Creature — Human Avatar Ally 4/4
 * //  — Legendary Creature — Avatar Ally 6/6 (transform DFC, TLA 207).
 *
 * Front — Avatar Aang:
 *   Flying, firebending 2
 *   Whenever you waterbend, earthbend, firebend, or airbend, draw a card. Then if you've done all
 *   four this turn, transform Avatar Aang.
 *
 * Back — Aang, Master of Elements:
 *   Flying
 *   Spells you cast cost {W}{U}{B}{R}{G} less to cast. (This can reduce generic costs.)
 *   At the beginning of each upkeep, you may transform Aang, Master of Elements. If you do, you gain
 *   4 life, draw four cards, put four +1/+1 counters on him, and he deals 4 damage to each opponent.
 *
 * The four-bend trigger is the general [Triggers.YouBend] (fires once per waterbend/earthbend/
 * firebend/airbend); "if you've done all four this turn" reads
 * [TurnTracker.DISTINCT_BENDS] (== 4). Aang's own `firebending 2` counts as the firebend. The
 * `{W}{U}{B}{R}{G}` reduction is [CostModification.ReduceColoredPerUnit] with a fixed unit — its
 * excess overflows to generic, matching "(This can reduce generic costs.)". The upkeep payoff is a
 * single [MayEffect]: choosing to transform performs the transform *and* the reward together.
 */
private val AangMasterOfElements = card("Aang, Master of Elements") {
    manaCost = ""
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Avatar Ally"
    oracleText = "Flying\n" +
        "Spells you cast cost {W}{U}{B}{R}{G} less to cast. (This can reduce generic costs.)\n" +
        "At the beginning of each upkeep, you may transform Aang, Master of Elements. If you do, " +
        "you gain 4 life, draw four cards, put four +1/+1 counters on him, and he deals 4 damage to " +
        "each opponent."
    power = 6
    toughness = 6

    keywords(Keyword.FLYING)

    // Spells you cast cost {W}{U}{B}{R}{G} less to cast. (This can reduce generic costs.)
    // ReduceColoredPerUnit removes each colored symbol where present and overflows the rest to
    // generic — exactly the "can reduce generic costs" rider.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceColoredPerUnit("{W}{U}{B}{R}{G}", CostReductionSource.Fixed(1))
        )
    }

    // At the beginning of each upkeep, you may transform Aang, Master of Elements. If you do, ...
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = MayEffect(
            Effects.Composite(
                listOf(
                    TransformEffect(EffectTarget.Self),
                    Effects.GainLife(4),
                    Effects.DrawCards(4),
                    Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 4, EffectTarget.Self),
                    Effects.DealDamage(4, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self)
                )
            ),
            descriptionOverride = "You may transform Aang, Master of Elements. If you do, you gain 4 " +
                "life, draw four cards, put four +1/+1 counters on him, and he deals 4 damage to each opponent."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Fahmi Fauzi"
        imageUri = "https://cards.scryfall.io/normal/back/f/e/fe29e909-50e9-4f04-b1a3-2cc5d7e3efe8.jpg?1778914169"
    }
}

private val AvatarAangFront = card("Avatar Aang") {
    manaCost = "{R}{G}{W}{U}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Avatar Ally"
    oracleText = "Flying, firebending 2\n" +
        "Whenever you waterbend, earthbend, firebend, or airbend, draw a card. Then if you've done " +
        "all four this turn, transform Avatar Aang."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)
    firebending(2)

    // Whenever you waterbend, earthbend, firebend, or airbend, draw a card. Then if you've done all
    // four this turn, transform. The bend that fired this trigger already folded into
    // DISTINCT_BENDS, so the fourth distinct bend of the turn sees the count at 4 here.
    triggeredAbility {
        trigger = Triggers.YouBend()
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(1),
                ConditionalEffect(
                    condition = Conditions.CompareAmounts(
                        DynamicAmount.TurnTracking(Player.You, TurnTracker.DISTINCT_BENDS),
                        ComparisonOperator.GTE,
                        DynamicAmount.Fixed(4)
                    ),
                    effect = TransformEffect(EffectTarget.Self)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Fahmi Fauzi"
        flavorText = "When the world needed him most, the Avatar returned."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe29e909-50e9-4f04-b1a3-2cc5d7e3efe8.jpg?1778914169"
    }
}

val AvatarAang: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AvatarAangFront,
    backFace = AangMasterOfElements,
)
