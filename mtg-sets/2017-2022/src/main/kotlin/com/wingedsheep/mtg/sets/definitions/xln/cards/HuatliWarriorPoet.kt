package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Huatli, Warrior Poet
 * {3}{R}{W}
 * Legendary Planeswalker — Huatli
 * Starting Loyalty: 3
 *
 * +2: You gain life equal to the greatest power among creatures you control.
 * 0: Create a 3/3 green Dinosaur creature token with trample.
 * −X: Huatli deals X damage divided as you choose among any number of target creatures.
 *     Creatures dealt damage this way can't block this turn.
 *
 * The −X announces X, then its targets and the division as it is activated (each target at least
 * 1, so never more targets than X; with X = 0 there are none). "Dealt damage this way" is what the
 * division actually reached — [Effects.DividedDamage]'s `storeDamagedAs` — so a target that became
 * illegal or whose share was prevented can still block.
 */
val HuatliWarriorPoet = card("Huatli, Warrior Poet") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Huatli"
    startingLoyalty = 3
    oracleText = "+2: You gain life equal to the greatest power among creatures you control.\n" +
        "0: Create a 3/3 green Dinosaur creature token with trample.\n" +
        "−X: Huatli deals X damage divided as you choose among any number of target creatures. " +
        "Creatures dealt damage this way can't block this turn."

    loyaltyAbility(+2) {
        effect = Effects.GainLife(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower())
        description = "You gain life equal to the greatest power among creatures you control."
    }

    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Dinosaur"),
            keywords = setOf(Keyword.TRAMPLE),
        )
        description = "Create a 3/3 green Dinosaur creature token with trample."
    }

    loyaltyAbilityX {
        targets(
            TargetFilter.Creature,
            minCount = 0,
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.xValue(),
        )
        effect = Effects.Pipeline {
            val damaged = runStoringCollection {
                Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue(), storeDamagedAs = it)
            }
            run(Effects.ForEachInCollection(damaged, Effects.CantBlock(EffectTarget.IterationEntity)))
        }
        description = "Huatli deals X damage divided as you choose among any number of target creatures. " +
            "Creatures dealt damage this way can't block this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "224"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0cc61ba-e91a-4b30-841c-cae8dd288b40.jpg?1783935711"
        ruling("2017-09-29", "The greatest power among creatures you control is determined as Huatli's first ability resolves. If that number is negative, you won't gain or lose any life.")
        ruling("2017-09-29", "You can activate Huatli's first ability even if you control no creatures. You simply won't gain any life.")
        ruling("2017-09-29", "You divide the damage among the target creatures as you activate Huatli's last ability. Each target must be assigned at least 1 damage. If X is 0, you can't choose any targets.")
        ruling("2017-09-29", "If some (but not all) of the targets become illegal, the original division of damage still applies, but no damage is dealt to illegal targets. If all targets become illegal, the ability won't resolve.")
    }
}
