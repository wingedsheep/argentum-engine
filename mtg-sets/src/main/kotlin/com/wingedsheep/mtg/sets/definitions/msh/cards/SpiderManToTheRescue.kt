package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Spider-Man, To the Rescue — Marvel Super Heroes #228
 * {2}{G/W} · Legendary Creature — Spider Human Hero · 3/2
 *
 * Flash
 * Vigilance, reach
 * No One Dies! — When Spider-Man enters, you may tap him. When you do, another target
 * nonattacking creature you control gains indestructible until end of turn.
 *
 * Modeling notes:
 *  - "No One Dies!" is a flavor ability word (CR 207.2c) with no rules meaning; it stays in the
 *    oracle text only.
 *  - The two-stage "you may tap him. When you do, …" is a reflexive trigger (CR 603.11):
 *    [ReflexiveTriggerEffect] with the optional tap as the `action` and the indestructible grant
 *    as the `reflexiveEffect`. That ordering matters — declining the tap (or Spider-Man already
 *    being tapped/gone when the ETB resolves) means the reflexive trigger never goes on the stack,
 *    so no target is ever chosen. Modeling it as one trigger with an optional target would have
 *    picked the target too early, at ETB-trigger time.
 *  - Vigilance doesn't interfere: the tap here is an effect, not an attack cost.
 *  - "another target nonattacking creature you control" — `excludeSelf` covers "another" (other
 *    than Spider-Man, the reflexive trigger's source) and the negated
 *    [StatePredicate.IsAttacking] covers "nonattacking". There is no `notAttacking()` builder, so
 *    the predicate is spelled out; dropping it (as a mechanical read of the text might) would
 *    wrongly let the rescue save an attacker mid-combat.
 */
val SpiderManToTheRescue = card("Spider-Man, To the Rescue") {
    manaCost = "{2}{G/W}"
    colorIdentity = "WG"
    typeLine = "Legendary Creature — Spider Human Hero"
    oracleText = "Flash\n" +
        "Vigilance, reach\n" +
        "No One Dies! — When Spider-Man enters, you may tap him. When you do, another target " +
        "nonattacking creature you control gains indestructible until end of turn. (Damage and " +
        "effects that say \"destroy\" don't destroy it.)"
    power = 3
    toughness = 2

    keywords(Keyword.FLASH, Keyword.VIGILANCE, Keyword.REACH)

    // No One Dies! — When Spider-Man enters, you may tap him. When you do, another target
    // nonattacking creature you control gains indestructible until end of turn.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Tap(EffectTarget.Self),
            optional = true,
            reflexiveEffect = Effects.GrantKeyword(
                Keyword.INDESTRUCTIBLE,
                EffectTarget.ContextTarget(0),
                Duration.EndOfTurn,
            ),
            reflexiveTargetRequirements = listOf(
                TargetCreature(
                    filter = TargetFilter(
                        baseFilter = GameObjectFilter.Creature.youControl().copy(
                            statePredicates = listOf(StatePredicate.Not(StatePredicate.IsAttacking))
                        ),
                        excludeSelf = true,
                    )
                )
            ),
            descriptionOverride = "You may tap Spider-Man. When you do, another target " +
                "nonattacking creature you control gains indestructible until end of turn.",
        )
        description = "No One Dies! — When Spider-Man enters, you may tap him. When you do, " +
            "another target nonattacking creature you control gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5db6968-1ca3-46bd-8cf9-e2c411ab29c1.jpg?1783902898"
    }
}
