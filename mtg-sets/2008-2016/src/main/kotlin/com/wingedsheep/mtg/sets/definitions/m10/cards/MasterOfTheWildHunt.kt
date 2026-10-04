package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Master of the Wild Hunt
 * {2}{G}{G}
 * Creature — Human Shaman
 * 3/3
 *
 * At the beginning of your upkeep, create a 2/2 green Wolf creature token.
 * {T}: Tap all untapped Wolf creatures you control. Each Wolf tapped this way deals damage equal to
 * its power to target creature. That creature deals damage equal to its power divided as its
 * controller chooses among any number of those Wolves.
 *
 * The Wolves are tapped by the effect, not as a cost, and aren't targeted — so the target's
 * controller divides its damage as the ability resolves.
 */
val MasterOfTheWildHunt = card("Master of the Wild Hunt") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    oracleText = "At the beginning of your upkeep, create a 2/2 green Wolf creature token.\n" +
        "{T}: Tap all untapped Wolf creatures you control. Each Wolf tapped this way deals damage " +
        "equal to its power to target creature. That creature deals damage equal to its power divided " +
        "as its controller chooses among any number of those Wolves."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf")
        )
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val wolves = gather(
                CardSource.ControlledPermanents(
                    Player.You,
                    GameObjectFilter.Creature.withSubtype(Subtype.WOLF).untapped()
                )
            )
            run(Effects.TapCollection(wolves))
            run(
                Effects.ForEachInCollection(
                    wolves,
                    Effects.DealDamage(
                        amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                        target = creature,
                        damageSource = EffectTarget.IterationEntity
                    )
                )
            )
            run(
                Effects.DistributeDamageAmongCollection(
                    amount = DynamicAmounts.powerOf(creature),
                    among = wolves,
                    damageSource = creature,
                    chooser = Chooser.ControllerOfTarget
                )
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "191"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cf3169c-f01c-47f4-ba4b-e199b7ade5fc.jpg?1783942361"
    }
}
