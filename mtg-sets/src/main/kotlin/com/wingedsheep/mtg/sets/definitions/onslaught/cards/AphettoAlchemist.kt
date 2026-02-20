package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Aphetto Alchemist
 * {1}{U}
 * Creature — Human Wizard
 * 1/2
 * {T}: Untap target artifact or creature.
 * Morph {U}
 */
val AphettoAlchemist = card("Aphetto Alchemist") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "{T}: Untap target artifact or creature.\nMorph {U}"

    activatedAbility {
        cost = AbilityCost.Tap
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature)
        )
        effect = TapUntapEffect(
            target = EffectTarget.ContextTarget(0),
            tap = false
        )
    }

    morph = "{U}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Ron Spears"
        flavorText = "He brews trouble."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfd2628f-63c4-4e19-83ea-26041650faab.jpg?1562948302"
    }
}
