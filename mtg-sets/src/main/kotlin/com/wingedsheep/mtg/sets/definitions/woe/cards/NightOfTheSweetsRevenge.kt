package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Night of the Sweets' Revenge
 * {3}{G}
 * Enchantment
 *
 * When this enchantment enters, create a Food token.
 * Foods you control have "{T}: Add {G}."
 * {5}{G}{G}, Sacrifice this enchantment: Creatures you control get +X/+X until end of turn, where X
 * is the number of Foods you control. Activate only as a sorcery.
 *
 * Three abilities, each on an existing primitive:
 *
 * 1. The ETB is the shared predefined Food token behind [Effects.CreateFood].
 * 2. "Foods you control have …" is a [GrantActivatedAbility] over a battlefield-scoped
 *    [GroupFilter] — the same lord-style ability grant as Citanul Hierophants, filtered to the Food
 *    artifact subtype rather than creatures. The granted ability is flagged
 *    `isManaAbility`/[TimingRule.ManaAbility] so it never uses the stack and is available while
 *    paying costs, including the {5}{G}{G} of this card's own last ability.
 * 3. The finisher is a [Effects.ForEachInGroup] over creatures you control, each getting a
 *    dynamically-sized until-end-of-turn buff. Per the Scryfall ruling, X is locked in as the
 *    ability *resolves*: the amount is evaluated once at resolution and baked into the
 *    layer-7c modification, so later Food changes don't move it, and creatures that arrive after
 *    resolution are never in the iterated group.
 *
 * A subtlety worth noting: sacrificing the enchantment is part of the *cost*, so the granted
 * "{T}: Add {G}" is gone by the time the buff resolves — but the Foods that were tapped for it are
 * still on the battlefield and still counted by X. Tapping a Food does not sacrifice it.
 */
val NightOfTheSweetsRevenge = card("Night of the Sweets' Revenge") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Foods you control have \"{T}: Add {G}.\"\n" +
        "{5}{G}{G}, Sacrifice this enchantment: Creatures you control get +X/+X until end of turn, " +
        "where X is the number of Foods you control. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.GREEN),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            ),
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype("Food").youControl()),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{G}{G}"), Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Artifact.withSubtype("Food"),
                ).count(),
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Artifact.withSubtype("Food"),
                ).count(),
                EffectTarget.Self,
            ),
        )
        description = "Creatures you control get +X/+X until end of turn, where X is the number of " +
            "Foods you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Leonardo Santanna"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27f53bed-7075-4303-aa9e-fcca0a266e19.jpg?1783915080"

        ruling(
            "2023-09-01",
            "The value of X is determined only as Night of the Sweets' Revenge's last ability " +
                "resolves. It won't change later in the turn if the number of Foods you control changes."
        )
        ruling(
            "2023-09-01",
            "Night of the Sweets' Revenge's last ability affects only creatures you control at the " +
                "time it resolves. Creatures you begin to control later in the turn won't be affected."
        )
        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token."
        )
    }
}
