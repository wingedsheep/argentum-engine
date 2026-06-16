package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Guardian Beast
 * {3}{B}
 * Creature — Beast
 * 2/4
 * As long as this creature is untapped, noncreature artifacts you control can't be enchanted,
 * they have indestructible, and other players can't gain control of them. This effect doesn't
 * remove Auras already attached to those artifacts.
 *
 * Three conditional static abilities, each gated on [Conditions.SourceIsUntapped] and scoped to
 * noncreature artifacts you control, grant a continuous property via the layer system:
 *  - INDESTRUCTIBLE (existing keyword).
 *  - CANT_BE_ENCHANTED — a granted restriction enforced at Aura-cast target legality (TargetValidator).
 *  - CANT_GAIN_CONTROL — enforced in the control-change executors (gain / exchange / by-most).
 * Because the grants only apply while Guardian Beast is untapped, Auras already attached when it
 * was untapped are not removed (the static doesn't detach anything) — matching the reminder text.
 */
val GuardianBeast = card("Guardian Beast") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 4
    oracleText = "As long as this creature is untapped, noncreature artifacts you control can't be " +
        "enchanted, they have indestructible, and other players can't gain control of them. This " +
        "effect doesn't remove Auras already attached to those artifacts."

    val noncreatureArtifactsYouControl =
        GroupFilter(GameObjectFilter.Artifact.notCreature().youControl())

    staticAbility {
        condition = Conditions.SourceIsUntapped
        ability = GrantKeyword(Keyword.INDESTRUCTIBLE, noncreatureArtifactsYouControl)
    }
    staticAbility {
        condition = Conditions.SourceIsUntapped
        ability = GrantKeyword(AbilityFlag.CANT_BE_ENCHANTED.name, noncreatureArtifactsYouControl)
    }
    staticAbility {
        condition = Conditions.SourceIsUntapped
        ability = GrantKeyword(AbilityFlag.CANT_GAIN_CONTROL.name, noncreatureArtifactsYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Ken Meyer, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9941f83b-2903-4eab-ac6d-5313e3978fa3.jpg?1562923479"
    }
}
