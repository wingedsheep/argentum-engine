package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.BecomeArtifactEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hydro-Man, Fluid Felon — Marvel's Spider-Man #33
 * {U}{U} · Legendary Creature — Elemental Villain · 2/2
 *
 * Whenever you cast a blue spell, if Hydro-Man is a creature, he gets +1/+1 until end of turn.
 * At the beginning of your end step, untap Hydro-Man. Until your next turn, he becomes a land and
 * gains "{T}: Add {U}." (He's not a creature during that time.)
 *
 * The end-step transform uses `BecomeArtifactEffect(cardTypes = setOf("LAND"), …)` — a general
 * "becomes [types]" effect — with `UntilYourNextTurn` duration; the granted "{T}: Add {U}" now
 * correctly expires via `CleanupPhaseManager.expireUntilYourNextTurnEffects`.
 */
val HydroManFluidFelon = card("Hydro-Man, Fluid Felon") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Elemental Villain"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a blue spell, if Hydro-Man is a creature, he gets +1/+1 until " +
        "end of turn.\n" +
        "At the beginning of your end step, untap Hydro-Man. Until your next turn, he becomes a " +
        "land and gains \"{T}: Add {U}.\" (He's not a creature during that time.)"

    // Whenever you cast a blue spell, if Hydro-Man is a creature, he gets +1/+1 until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.BLUE))
        triggerCondition = Conditions.SourceMatches(GameObjectFilter.Creature)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    // End step: untap, then become a non-creature land with "{T}: Add {U}" until your next turn.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.Composite(
            Effects.Untap(EffectTarget.Self),
            BecomeArtifactEffect(
                target = EffectTarget.Self,
                cardTypes = setOf("LAND"),
                colors = null,          // keep Hydro-Man's blue color
                loseAllAbilities = false,
                grantedAbility = ActivatedAbility(
                    cost = Costs.Tap,
                    effect = Effects.AddMana(Color.BLUE),
                    isManaAbility = true,
                    descriptionOverride = "{T}: Add {U}."
                ),
                duration = Duration.UntilYourNextTurn
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Borja Pindado"
        flavorText = "\"Well, I always wanted a waterfront view.\"\n—Spider-Man"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e53115a4-8959-40fa-b763-931504a1c5a2.jpg?1783905353"
    }
}
