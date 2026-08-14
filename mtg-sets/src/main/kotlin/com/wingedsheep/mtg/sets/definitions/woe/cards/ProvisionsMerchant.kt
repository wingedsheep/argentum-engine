package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Provisions Merchant
 * {2}{G}{G}
 * Creature — Beast Peasant
 * 3/3
 *
 * When this creature enters, create a Food token.
 * Whenever this creature attacks, you may sacrifice a Food. If you do, attacking creatures get
 * +1/+1 and gain trample until end of turn.
 *
 * The attack ability is the [BristlebudFarmer] shape: a [MayEffect] wrapping
 * `Sacrifice(Food).then(payoff)`, so declining — or simply controlling no Food — skips the payoff
 * without the ability fizzling. "If you do" (not "When you do") means the pump happens in the same
 * resolution rather than as a reflexive trigger.
 *
 * The pump is over [Filters.Group.attackingCreatures] — *every* attacking creature, not just the
 * Merchant's controller's, which is what the oracle says and what matters once a third player is in
 * the game. The set is snapshotted as the effect resolves, so a creature that somehow starts
 * attacking later in the combat is not retroactively included.
 *
 * A `feasibility` gate suppresses the prompt when the controller has no Food. Without it a 3/3 that
 * attacks every turn would raise an unanswerable "sacrifice a Food?" question every combat.
 */
val ProvisionsMerchant = card("Provisions Merchant") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast Peasant"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, create a Food token. (It's an artifact with \"{2}, " +
        "{T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever this creature attacks, you may sacrifice a Food. If you do, attacking creatures " +
        "get +1/+1 and gain trample until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MayEffect(
            Effects.Sacrifice(
                GameObjectFilter.Artifact.withSubtype("Food"),
                count = 1,
                target = EffectTarget.Controller
            ).then(
                Effects.Composite(
                    Patterns.Group.modifyStatsForAll(
                        power = 1,
                        toughness = 1,
                        filter = Filters.Group.attackingCreatures
                    ),
                    Patterns.Group.grantKeywordToAll(
                        Keyword.TRAMPLE,
                        Filters.Group.attackingCreatures
                    )
                )
            ),
            // The Merchant attacks every turn; without a Food the question has no answer, so don't
            // ask it. Purely an engine-decidable precondition — it never pre-empts a real choice.
            feasibility = FeasibilityCheck.ControlsPermanentMatching(
                GameObjectFilter.Artifact.withSubtype("Food")
            )
        )
        description = "You may sacrifice a Food. If you do, attacking creatures get +1/+1 and gain " +
            "trample until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "321"
        artist = "Raluca Marinescu"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a015282d-bcd4-44ab-ae26-41e4e3b23fc0.jpg?1783915038"

        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact " +
                "token. For example, you can sacrifice Tough Cookie (an Artifact Creature — Food " +
                "Golem) to activate an ability with \"Sacrifice a Food\" in its cost."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a " +
                "creature type."
        )
    }
}
