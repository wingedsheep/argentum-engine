package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Killmonger, Scourge of Wakanda — Marvel Super Heroes #218
 * {2}{B}{G} · Legendary Creature — Human Mercenary Villain · 3/3
 *
 * When Killmonger enters, you may sacrifice another creature. When you do, destroy target
 * nonland permanent an opponent controls.
 * As long as there are two or more creature cards in your graveyard, Killmonger gets +2/+1.
 *
 * Modeling notes:
 *  - Ruthless Lawbringer's shape exactly, narrowed to permanents an opponent controls. The
 *    sacrifice is a resolution-time *choice*, not a target: [ReflexiveTriggerEffect] takes the
 *    optional yes/no first, then a [SelectTargetEffect] picks which creature, so declining never
 *    forces a commitment. Only if a creature is actually sacrificed does the "When you do"
 *    reflexive ability trigger — and its target is chosen as that second ability goes on the
 *    stack, per CR 603.11 (a reflexive trigger targets when it's put on the stack, not up front).
 *  - Killmonger himself is excluded from the sacrifice pool via `.other()` ("another creature").
 *  - "As long as …" is a continuous conditional static ability: `ModifyStats(+2, +1)` scoped to
 *    the source, gated by [Conditions.CreatureCardsInGraveyardAtLeast]. The `condition = …` slot on
 *    `staticAbility { }` wraps it in a `ConditionalStaticAbility`, so the projector re-evaluates the
 *    graveyard every time state changes rather than locking the bonus in once.
 */
val KillmongerScourgeOfWakanda = card("Killmonger, Scourge of Wakanda") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Mercenary Villain"
    power = 3
    toughness = 3
    oracleText = "When Killmonger enters, you may sacrifice another creature. When you do, destroy " +
        "target nonland permanent an opponent controls.\n" +
        "As long as there are two or more creature cards in your graveyard, Killmonger gets +2/+1."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(
                listOf(
                    SelectTargetEffect(
                        requirement = TargetObject(filter = TargetFilter.CreatureYouControl.other()),
                        storeAs = "killmongerSacrifice",
                    ),
                    Effects.SacrificeTarget(EffectTarget.PipelineTarget("killmongerSacrifice")),
                ),
            ),
            optional = true,
            reflexiveEffect = Effects.Destroy(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetPermanent(filter = TargetFilter.NonlandPermanentOpponentControls),
            ),
            descriptionOverride = "You may sacrifice another creature. When you do, destroy target " +
                "nonland permanent an opponent controls.",
        )
        description = "When Killmonger enters, you may sacrifice another creature. When you do, " +
            "destroy target nonland permanent an opponent controls."
    }

    staticAbility {
        ability = ModifyStats(2, 1, Filters.Self)
        condition = Conditions.CreatureCardsInGraveyardAtLeast(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Sean Vo"
        flavorText = "\"I was stolen from paradise, but paradise never came looking for me.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5060aa13-4b33-4b3a-8bdb-dd81308fa3e3.jpg?1783902900"
    }
}
