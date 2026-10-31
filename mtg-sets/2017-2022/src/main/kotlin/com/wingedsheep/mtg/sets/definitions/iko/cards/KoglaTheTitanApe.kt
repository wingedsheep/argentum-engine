package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kogla, the Titan Ape — Ikoria: Lair of Behemoths #162
 * {3}{G}{G}{G} · Legendary Creature — Ape 7/6
 *
 * When Kogla enters, it fights up to one target creature you don't control.
 * Whenever Kogla attacks, destroy target artifact or enchantment defending player controls.
 * {1}{G}: Return target Human you control to its owner's hand. Kogla gains indestructible until
 * end of turn.
 *
 * "Defending player" is `Player.DefendingPlayer` inside the target filter — the player Kogla is
 * attacking, not every defending seat (Silumgar, the Drifting Death's shape). The bounce targets,
 * so an illegal Human fizzles the whole ability and Kogla gains nothing (2020-04-17 ruling).
 */
val KoglaTheTitanApe = card("Kogla, the Titan Ape") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Ape"
    power = 7
    toughness = 6
    oracleText = "When Kogla enters, it fights up to one target creature you don't control.\n" +
        "Whenever Kogla attacks, destroy target artifact or enchantment defending player controls.\n" +
        "{1}{G}: Return target Human you control to its owner's hand. Kogla gains indestructible " +
        "until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            optional = true
        )
        effect = Effects.Fight(EffectTarget.Self, theirs)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val permanent = target(
            TargetFilter(
                GameObjectFilter.ArtifactOrEnchantment
                    .targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
            )
        )
        effect = Effects.Destroy(permanent)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        val human = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Human").youControl()))
        effect = Effects.ReturnToHand(human) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
        description = "{1}{G}: Return target Human you control to its owner's hand. " +
            "Kogla gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c35ca79-eb72-427a-a8ed-404b2214389a.jpg?1783931034"
        ruling(
            "2020-04-17",
            "If the target creature is an illegal target when Kogla's first ability tries to resolve, the " +
                "ability doesn't resolve. If it's a legal target but Kogla is no longer on the battlefield " +
                "when the ability resolves, the target creature won't deal or be dealt damage."
        )
        ruling(
            "2020-04-17",
            "If the target Human is an illegal target by the time Kogla's last ability tries to resolve, " +
                "the ability won't resolve. Kogla won't gain indestructible."
        )
    }
}
