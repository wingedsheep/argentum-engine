package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kellan, Planar Trailblazer
 * {R}
 * Legendary Creature — Human Faerie Scout
 * 2/1
 *
 * {1}{R}: If Kellan is a Scout, it becomes a Human Faerie Detective and gains "Whenever Kellan
 * deals combat damage to a player, exile the top card of your library. You may play that card
 * this turn."
 * {2}{R}: If Kellan is a Detective, it becomes a 3/2 Human Faerie Rogue and gains double strike.
 *
 * Implementation notes — a two-step self-upgrade chain, both steps modeled as ordinary activated
 * abilities whose *whole* body is gated on Kellan's current creature type:
 *
 * - **The "If Kellan is a …" clause is a resolution-time state test, not an activation
 *   restriction** (Scryfall ruling: "You can activate Kellan's abilities regardless of what
 *   creature types he currently has. Each ability checks Kellan's creature types when it
 *   resolves."). So each ability is freely activatable and its body is wrapped in a
 *   [ConditionalEffect] over [Conditions.SourceHasSubtype] — which lowers to a
 *   `Gate.WhenCondition` and evaluates the source against **projected** state, so step 2 sees the
 *   Detective type that step 1's Layer-4 type change conferred. Activating out of order (or
 *   twice) is legal and simply does nothing.
 * - **The type changes replace, they don't add** (ruling: "Kellan's abilities overwrite its
 *   existing creature types … He won't have the Scout creature type"), so both steps use
 *   [Effects.SetCreatureSubtypes] rather than `AddCreatureType`. Step 1 dropping Scout for
 *   Detective is also what makes the chain one-way: once step 1 has resolved, step 1 can never
 *   apply again.
 * - **Nothing here has a duration** (ruling: "Neither of these abilities have durations"), so
 *   every grant is [Duration.Permanent] — it lasts until Kellan leaves the battlefield or a later
 *   effect overwrites it. The 3/2 is [Effects.SetBasePowerAndToughness] (Layer 7b, *setting*),
 *   which by ordinary layer/timestamp rules overwrites earlier set-P/T effects but leaves
 *   +N/+N modifiers and counters (Layer 7c) intact — exactly the fourth ruling.
 * - The granted trigger is the ordinary impulse-draw shape: [Triggers.DealsCombatDamageToPlayer]
 *   (SELF binding, so it fires on Kellan's own combat damage) over
 *   [Patterns.Exile.impulse], conferred onto Kellan itself by a one-shot
 *   [GrantTriggeredAbilityEffect]. Both step-1 halves are permanent and independent: a later
 *   effect that re-sets Kellan's types doesn't strip the granted trigger.
 */
val KellanPlanarTrailblazer = card("Kellan, Planar Trailblazer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Faerie Scout"
    power = 2
    toughness = 1
    oracleText = "{1}{R}: If Kellan is a Scout, it becomes a Human Faerie Detective and gains " +
        "\"Whenever Kellan deals combat damage to a player, exile the top card of your library. " +
        "You may play that card this turn.\"\n" +
        "{2}{R}: If Kellan is a Detective, it becomes a 3/2 Human Faerie Rogue and gains double strike."

    // {1}{R}: If Kellan is a Scout, it becomes a Human Faerie Detective and gains "Whenever
    // Kellan deals combat damage to a player, exile the top card of your library. You may play
    // that card this turn."
    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = ConditionalEffect(
            condition = Conditions.SourceHasSubtype(Subtype.SCOUT),
            effect = Effects.Composite(
                Effects.SetCreatureSubtypes(
                    subtypes = setOf(Subtype.HUMAN.value, Subtype.FAERIE.value, Subtype.DETECTIVE.value),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                ),
                GrantTriggeredAbilityEffect(
                    ability = TriggeredAbility.create(
                        trigger = Triggers.DealsCombatDamageToPlayer.event,
                        binding = Triggers.DealsCombatDamageToPlayer.binding,
                        effect = Patterns.Exile.impulse(count = 1, storeAs = "kellanImpulseExiled"),
                        descriptionOverride = "Whenever Kellan deals combat damage to a player, " +
                            "exile the top card of your library. You may play that card this turn."
                    ),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                )
            )
        )
        description = "If Kellan is a Scout, it becomes a Human Faerie Detective and gains " +
            "\"Whenever Kellan deals combat damage to a player, exile the top card of your " +
            "library. You may play that card this turn.\""
    }

    // {2}{R}: If Kellan is a Detective, it becomes a 3/2 Human Faerie Rogue and gains double strike.
    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = ConditionalEffect(
            condition = Conditions.SourceHasSubtype(Subtype.DETECTIVE),
            effect = Effects.Composite(
                Effects.SetBasePowerAndToughness(
                    power = 3,
                    toughness = 2,
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                ),
                Effects.SetCreatureSubtypes(
                    subtypes = setOf(Subtype.HUMAN.value, Subtype.FAERIE.value, Subtype.ROGUE.value),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                ),
                Effects.GrantKeyword(
                    keyword = Keyword.DOUBLE_STRIKE,
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                )
            )
        )
        description = "If Kellan is a Detective, it becomes a 3/2 Human Faerie Rogue and gains double strike."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f46a9329-7b91-441d-8653-50c1152c9120.jpg?1783909101"
        ruling(
            "2024-11-08",
            "Neither of these abilities have durations. If one of them resolves, it will remain in " +
                "effect until the game ends, Kellan leaves the battlefield, or some subsequent effect " +
                "changes its characteristics, whichever comes first.",
        )
        ruling(
            "2024-11-08",
            "Kellan's abilities overwrite its existing creature types. For example, once Kellan's " +
                "first ability resolves, if he was a Scout when it resolved, Kellan will be a Human " +
                "Faerie Detective. He won't have the Scout creature type.",
        )
        ruling(
            "2024-11-08",
            "You can activate Kellan's abilities regardless of what creature types he currently has. " +
                "Each ability checks Kellan's creature types when it resolves. If Kellan doesn't have " +
                "the appropriate creature type at that time, the ability will do nothing.",
        )
        ruling(
            "2024-11-08",
            "The effects from Kellan's abilities overwrite other effects that set power and/or " +
                "toughness if and only if those effects existed before the ability resolved. They will " +
                "not overwrite effects that modify power or toughness without setting it (whether from " +
                "a static ability, counters, or a resolved spell or ability), nor will they overwrite " +
                "effects that set power and toughness which come into existence after they resolve. " +
                "Effects that switch the creature's power and toughness are always applied after any " +
                "other power or toughness changing effects, including these two, regardless of the " +
                "order in which they are created.",
        )
    }
}
