package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agrus Kos, Eternal Soldier
 * {3}{W}
 * Legendary Creature — Spirit Soldier
 * 3/4
 *
 * Vigilance
 * Whenever Agrus Kos becomes the target of an ability that targets only it, you may pay {1}{R/W}.
 * If you do, copy that ability for each other creature you control that ability could target. Each
 * copy targets a different one of those creatures.
 *
 * - The trigger is `becomesTarget(abilitiesOnly, targetsOnlyIt)`: every instance of "target" on the
 *   ability must be Agrus Kos, so an ability with several targets all pointed here still triggers it
 *   (and each copy then fills every slot with its one creature). It doesn't matter who controls the
 *   ability.
 * - The payoff is the CR 707.10d copy-for-each-could-target shape on [EffectTarget.TargetingSource]
 *   with the default `copier = You` — Agrus Kos's controller controls every copy "no matter which
 *   player controlled the original ability", and the candidates are that player's creatures.
 */
val AgrusKosEternalSoldier = card("Agrus Kos, Eternal Soldier") {
    manaCost = "{3}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Spirit Soldier"
    power = 3
    toughness = 4
    oracleText = "Vigilance\n" +
        "Whenever Agrus Kos becomes the target of an ability that targets only it, you may pay " +
        "{1}{R/W}. If you do, copy that ability for each other creature you control that ability " +
        "could target. Each copy targets a different one of those creatures. ({R/W} can be paid " +
        "with either {R} or {W}.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(abilitiesOnly = true, targetsOnlyIt = true)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}{R/W}"),
            then = Effects.CopyForEachOtherPossibleTarget(
                candidates = GameObjectFilter.Creature.youControl(),
                target = EffectTarget.TargetingSource
            )
        )
        description = "Whenever Agrus Kos becomes the target of an ability that targets only it, " +
            "you may pay {1}{R/W}. If you do, copy that ability for each other creature you control " +
            "that ability could target. Each copy targets a different one of those creatures."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea531418-6c7c-4e23-b681-6bfdd4a3eb79.jpg?1783919197"

        ruling("2022-12-02", "The last ability triggers whenever Agrus Kos, Eternal Soldier becomes the target of an ability that targets it and no other object or player. It doesn't matter who controls the ability.")
        ruling("2022-12-02", "If an ability has multiple targets, but it's targeting Agrus Kos with all of them, the last ability triggers. In that case, each of the copies will also target only one of those creatures.")
        ruling("2022-12-02", "Any creature you control that can't be targeted by the copied ability (due to protection abilities, targeting restrictions, or any other reason) is just ignored. No copy is created for that creature.")
        ruling("2022-12-02", "The controller of Agrus Kos controls all the copies, no matter which player controlled the original ability. That player chooses the order the copies are put on the stack. The original ability will be on the stack beneath those copies and will resolve last.")
        ruling("2022-12-02", "If the ability that's copied is modal (that is, it says \"Choose one —\" or the like), the copies will have the same mode. Their controller can't choose a different one.")
        ruling("2022-12-02", "If the ability that's copied has an X whose value was determined as it was put on the stack, the copies have the same value of X.")
    }
}
