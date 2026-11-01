package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deputy of Detention
 * {1}{W}{U}
 * Creature — Vedalken Wizard
 * 1/3
 * When this creature enters, exile target nonland permanent an opponent controls and all other
 * nonland permanents that player controls with the same name as that permanent until this
 * creature leaves the battlefield.
 *
 * Maelstrom Pulse's same-name gather, scoped to the target's controller, feeding Idol of
 * Endurance's per-card [Effects.MoveUntilSourceLeaves]: a duration-scoped exile, not a linked
 * pile plus a leaves trigger, so if the Deputy is gone before the trigger resolves nothing is
 * exiled (2019-01-25 ruling). The target is exiled through its own handle and the same-named
 * permanents are gathered with `excludeChosenTargets`, so the untargeted ones are swept up even
 * with hexproof. The name is read only off a face-up target — a face-down permanent has no name
 * (2019-01-25 ruling), so it is exiled alone; face-down candidates never match a name either.
 */
val DeputyOfDetention = card("Deputy of Detention") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Vedalken Wizard"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, exile target nonland permanent an opponent controls and all " +
        "other nonland permanents that player controls with the same name as that permanent until this " +
        "creature leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val targetName = storeCardName(filter(chosen, GameObjectFilter.Any.faceUp()))
            val sameNamed = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.NonlandPermanent.namedFromVariable(targetName),
                    player = Player.ControllerOf("target nonland permanent"),
                    excludeChosenTargets = true,
                )
            )
            run(Effects.MoveUntilSourceLeaves(t, Zone.EXILE))
            run(
                Effects.ForEachInCollection(
                    sameNamed,
                    Effects.MoveUntilSourceLeaves(EffectTarget.IterationEntity, Zone.EXILE)
                )
            )
        }
        description = "When this creature enters, exile target nonland permanent an opponent controls and " +
            "all other nonland permanents that player controls with the same name as that permanent until " +
            "this creature leaves the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "G-host Lee"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e362055-78a1-48fa-a4ef-6cf7e0b21b14.jpg?1783933654"

        ruling(
            "2019-01-25",
            "If the target nonland permanent is an illegal target by the time this ability tries to resolve, " +
                "the ability doesn't resolve. Nothing is exiled."
        )
        ruling("2019-01-25", "If Deputy of Detention leaves the battlefield before its triggered ability resolves, nothing is exiled.")
        ruling(
            "2019-01-25",
            "This ability has only one target. The other permanents with that name aren't targeted. For example, " +
                "a permanent with hexproof will be exiled if it has the same name as the target nonland permanent."
        )
        ruling("2019-01-25", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling(
            "2019-01-25",
            "A face-down creature has no name, so it can't share a name with anything. This includes other " +
                "creatures with no name."
        )
    }
}
