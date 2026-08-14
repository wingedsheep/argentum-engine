package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Behold the Sinister Six! — Marvel's Spider-Man #51
 * {6}{B} · Sorcery
 *
 * Return up to six target creature cards with different names from your graveyard to the
 * battlefield.
 *
 * Uses the new `TargetObject.differentNames` cross-target constraint (enforced by `TargetValidator`
 * / `DecisionValidators`), so the six chosen cards can't include duplicates of the same name.
 */
val BeholdTheSinisterSix = card("Behold the Sinister Six!") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to six target creature cards with different names from your graveyard " +
        "to the battlefield."

    spell {
        target = TargetObject(
            count = 6,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard,
            differentNames = true,
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.PutOntoBattlefield(EffectTarget.ContextTarget(0)))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "51"
        artist = "Nathaniel Himawan"
        flavorText = "\"Open your eyes to your nightmare, Spider-Man.\"\n—Doctor Octopus, Otto Octavius"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1919bfec-1906-4178-ad32-d4589842e563.jpg?1783905348"
    }
}
