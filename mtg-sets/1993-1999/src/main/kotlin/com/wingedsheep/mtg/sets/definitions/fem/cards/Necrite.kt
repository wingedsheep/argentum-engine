package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Necrite
 * {1}{B}{B}
 * Creature — Thrull
 * 2/2
 * Whenever this creature attacks and isn't blocked, you may sacrifice it. If you do, destroy
 * target creature defending player controls. It can't be regenerated.
 *
 * The engine asks the "you may" before target selection for may-then-target triggers, so the
 * prompt names the target by its printed wording rather than as "that creature". [MindstabThrull]'s sibling.
 */
val Necrite = card("Necrite") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull"
    oracleText = "Whenever this creature attacks and isn't blocked, you may sacrifice it. If you " +
        "do, destroy target creature defending player controls. It can't be regenerated."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.AttacksAndIsntBlocked
        val t = target(
            "target creature defending player controls",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.copy(
                        controllerPredicate = ControllerPredicate.ControlledByReferencedPlayer(
                            EffectTarget.PlayerRef(Player.DefendingPlayer)
                        )
                    )
                )
            )
        )
        effect = MayEffect(
            Effects.Composite(
                SacrificeSelfEffect,
                Effects.Destroy(t, noRegenerate = true)
            ),
            descriptionOverride = "sacrifice this creature. If you do, destroy target creature " +
                "defending player controls. It can't be regenerated",
        )
        description = "Whenever this creature attacks and isn't blocked, you may sacrifice it. If you do, destroy target creature defending player controls. It can't be regenerated."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41a"
        artist = "Ron Spencer"
        flavorText = "Necrites killed Jherana Rure, ending the counter-insurgency."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/311d752a-ce8a-44cb-8aeb-1ed66705eb09.jpg?1783947900"
    }
}
