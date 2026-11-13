package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pathway Arrows (BFZ #225)
 * {1}
 * Artifact — Equipment
 * Equipped creature has "{2}, {T}: This creature deals 1 damage to target creature. If a colorless
 * creature is dealt damage this way, tap it."
 * Equip {2}
 *
 * The Viridian Longbow grant (the equipped creature is the ability's source, pays the {T}, and deals
 * the damage) combined with Touch of the Void's "dealt damage this way" shape: the damage records what
 * it actually dealt (after prevention; 0 if the target became illegal), and only a target that was
 * really dealt damage *and* is colorless as the ability resolves gets tapped. "Colorless" is read off
 * the target (projected state, so a devoid creature counts), not off the equipped creature.
 */
val PathwayArrows = card("Pathway Arrows") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{2}, {T}: This creature deals 1 damage to target creature. " +
        "If a colorless creature is dealt damage this way, tap it.\"\n" +
        "Equip {2}"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
                val creature = target(TargetFilter.Creature)
                effect = Effects.Pipeline {
                    val dealt = runStoringNumber {
                        Effects.DealDamage(
                            1,
                            creature,
                            damageSource = EffectTarget.Self,
                            damageDealtVariable = it
                        )
                    }
                    ifThen(
                        Conditions.All(
                            Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GT, 0),
                            Conditions.EntityMatches(
                                creature,
                                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless)
                            ),
                        )
                    ) {
                        run(Effects.Tap(creature))
                    }
                }
            }
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Kev Walker"
        flavorText = "Also called \"pathway stones,\" hedron shards can immobilize Eldrazi on contact."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15b98611-a6f7-498c-87e3-893a3f4b4f73.jpg?1783938177"
    }
}
