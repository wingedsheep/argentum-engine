package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravager Wurm — Ravnica Allegiance #200
 * {3}{R}{G}{G} · Creature — Wurm · 4 / 5
 *
 * Riot, plus an ETB "choose up to one" modal trigger (`chooseCount = 1, minChooseCount = 0`):
 * fight a creature you don't control, or destroy a land that has a non-mana activated ability
 * ([CardPredicate.HasNonManaActivatedAbility], the same predicate Tsabo's Web reads).
 */
val RavagerWurm = card("Ravager Wurm") {
    manaCost = "{3}{R}{G}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Wurm"
    power = 4
    toughness = 5
    oracleText = "Riot (This creature enters with your choice of a +1/+1 counter or haste.)\n" +
        "When this creature enters, choose up to one —\n" +
        "• This creature fights target creature you don't control.\n" +
        "• Destroy target land with an activated ability that isn't a mana ability."

    riot()

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("This creature fights target creature you don't control") {
                    val theirs = target(
                        TargetFilter(
                            GameObjectFilter.Creature.copy(
                                controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                            )
                        )
                    )
                    effect = Effects.Fight(EffectTarget.Self, theirs)
                },
                mode("Destroy target land with an activated ability that isn't a mana ability") {
                    val land = target(
                        TargetFilter(
                            GameObjectFilter(
                                cardPredicates = listOf(
                                    CardPredicate.IsLand,
                                    CardPredicate.HasNonManaActivatedAbility
                                )
                            )
                        )
                    )
                    effect = Effects.Destroy(land)
                }
            ),
            chooseCount = 1,
            minChooseCount = 0,
            countsAsModalSpell = false
        )
        description = "When this creature enters, choose up to one — This creature fights target " +
            "creature you don't control; or destroy target land with an activated ability that " +
            "isn't a mana ability."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "200"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d5a6085-bb5d-412e-8831-757996bbc8fb.jpg"
    }
}
