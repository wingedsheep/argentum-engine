package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val AfflictedDeserterFront = card("Afflicted Deserter") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 3
    toughness = 2
    oracleText = "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(),
            ComparisonOperator.EQ,
            0,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "David Palumbo"
        flavorText = "The rising of the first full moon eliminated any doubt as to the horrible truth lurking within."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2c044c0-3625-4bdf-9445-b462394cecae.jpg?1783940829"
    }
}

private val WerewolfRansacker = card("Werewolf Ransacker") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 4
    oracleText = "Whenever this creature transforms into Werewolf Ransacker, you may destroy target artifact. " +
        "If that artifact is put into a graveyard this way, this creature deals 3 damage to that artifact's controller.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        // The target is chosen as the trigger goes on the stack; whether to destroy it is
        // decided on resolution.
        optional = true
        target(TargetFilter.Artifact)
        effect = Effects.Pipeline {
            val targets = gather(CardSource.ChosenTargets)
            // Snapshot the controller before the destroy strips it.
            val controllers = captureControllers(targets)
            // Indestructible / regenerated artifacts drop out of the moved collection; one a
            // replacement sent somewhere other than the graveyard is filtered out next. An artifact
            // token still sits in the graveyard here, until state-based actions remove it.
            val moved = moveTracked(targets, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy)
            val inGraveyard = filter(
                moved,
                GameObjectFilter.Any.withStatePredicate(StatePredicate.InZone(Zone.GRAVEYARD)),
            )
            forEachCaptured(inGraveyard, original = targets, controllers = controllers) {
                run(Effects.DealDamage(3, EffectTarget.Controller))
            }
        }
    }
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(),
            ComparisonOperator.GTE,
            2,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/back/a/2/a2c044c0-3625-4bdf-9445-b462394cecae.jpg?1783940829"
        ruling(
            "2013-07-01",
            "If the targeted artifact has indestructible or regenerates (or you choose not to destroy it), " +
                "Werewolf Ransacker doesn't deal damage to that artifact's controller. Similarly, if the targeted " +
                "artifact is destroyed but a replacement effect moves it to a different zone instead of its owner's " +
                "graveyard, Werewolf Ransacker doesn't deal damage to that artifact's controller.",
        )
        ruling(
            "2011-01-22",
            "An artifact token that's destroyed is put into its owner's graveyard before it ceases to exist. " +
                "If a token is destroyed by Werewolf Ransacker ability, Werewolf Ransacker deals damage to that token's controller.",
        )
    }
}

val AfflictedDeserter: CardDefinition =
    CardDefinition.doubleFacedCreature(AfflictedDeserterFront, WerewolfRansacker)
