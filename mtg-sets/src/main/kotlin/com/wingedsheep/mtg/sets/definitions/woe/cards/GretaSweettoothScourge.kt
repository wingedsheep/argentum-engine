package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Greta, Sweettooth Scourge
 * {1}{B}{G}
 * Legendary Creature — Human Warrior
 * 3/3
 *
 * When Greta enters, create a Food token.
 * {G}, Sacrifice a Food: Put a +1/+1 counter on target creature. Activate only as a sorcery.
 * {1}{B}, Sacrifice a Food: You draw a card and you lose 1 life.
 *
 * Three abilities over existing primitives; the only thing worth naming is the Food cost filter.
 * "Sacrifice a Food" means any Food *artifact*, not just a Food token (2024-11-08 ruling), so the
 * cost matches on the Food subtype the way [SweettoothWitch] does — Tough Cookie (an Artifact
 * Creature — Food Golem) is legal fodder. The engine's cost payment already forbids spending one
 * Food on two costs.
 *
 * The counter ability targets *any* creature, not just yours ([Targets.Creature]), and carries
 * [TimingRule.SorcerySpeed] for "Activate only as a sorcery." The draw ability has no such
 * restriction and no target — the life loss is the controller's, hence [EffectTarget.Controller]
 * rather than the `LoseLife` default of a target opponent.
 */
val GretaSweettoothScourge = card("Greta, Sweettooth Scourge") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "When Greta enters, create a Food token. (It's an artifact with \"{2}, {T}, " +
        "Sacrifice this token: You gain 3 life.\")\n" +
        "{G}, Sacrifice a Food: Put a +1/+1 counter on target creature. Activate only as a sorcery.\n" +
        "{1}{B}, Sacrifice a Food: You draw a card and you lose 1 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}"),
            Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food")),
        )
        timing = TimingRule.SorcerySpeed
        val creature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Put a +1/+1 counter on target creature. Activate only as a sorcery."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food")),
        )
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.LoseLife(1, EffectTarget.Controller),
        )
        description = "You draw a card and you lose 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cfd365e-34d1-4224-b925-119000311934.jpg?1783915071"

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
        ruling(
            "2024-11-08",
            "You can't sacrifice a Food to pay multiple costs. For example, you can't sacrifice a " +
                "Food token to activate its own ability and also to activate this ability."
        )
    }
}
