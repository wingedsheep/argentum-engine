package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aurelia's Fury — Gatecrash #144 (canonical printing)
 * {X}{R}{W} · Instant
 *
 * Aurelia's Fury deals X damage divided as you choose among any number of targets. Tap each
 * creature dealt damage this way. Players dealt damage this way can't cast noncreature spells
 * this turn.
 *
 * X is announced, then the targets and the division (each target at least 1, so X caps the target
 * count). "Dealt damage this way" is what the division actually reached — `storeDamagedAs` — so an
 * illegal target or a wholly prevented share is neither tapped nor stopped from casting. The
 * creatures in that collection are tapped (a damaged planeswalker is not a creature and stays
 * untapped); each player in it can't cast noncreature spells this turn (the loop rebinds the
 * controller to that player).
 */
val AureliasFury = card("Aurelia's Fury") {
    manaCost = "{X}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Aurelia's Fury deals X damage divided as you choose among any number of targets. " +
        "Tap each creature dealt damage this way. Players dealt damage this way can't cast noncreature " +
        "spells this turn."

    spell {
        val struck = targets(
            AnyTarget(
                minCount = 0,
                unlimited = true,
                dynamicMaxCount = DynamicAmounts.xValue(),
            )
        )
        effect = Effects.Pipeline {
            val damaged = runStoringCollection {
                Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue(), among = struck, storeDamagedAs = it)
            }
            val creatures = filter(damaged, GameObjectFilter.Creature)
            run(Effects.TapCollection(creatures))
            run(
                Effects.ForEachPlayer(
                    damaged.asPlayers,
                    Effects.CantCastSpells(EffectTarget.Controller, spellFilter = GameObjectFilter.Noncreature),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "144"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a3465b6-ee7f-4553-bbf1-85fae9734b67.jpg?1783940113"
        ruling(
            "2013-01-24",
            "You announce the value of X and how the damage will be divided as part of casting Aurelia's Fury. " +
                "Each chosen target must receive at least 1 damage."
        )
        ruling(
            "2013-01-24",
            "If Aurelia's Fury has multiple targets, and some but not all of them are illegal targets when " +
                "Aurelia's Fury resolves, Aurelia's Fury will still deal damage to the remaining legal targets " +
                "according to the original damage division."
        )
        ruling(
            "2013-01-24",
            "If all of the targets are illegal when Aurelia's Fury tries to resolve, it won't resolve and none " +
                "of its effects will happen. Nothing will be dealt damage."
        )
    }
}
