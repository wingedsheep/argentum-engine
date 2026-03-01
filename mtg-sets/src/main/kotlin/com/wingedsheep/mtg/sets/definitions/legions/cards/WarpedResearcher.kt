package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Warped Researcher
 * {4}{U}
 * Creature — Human Wizard Mutant
 * 3/4
 * Whenever a player cycles a card, Warped Researcher gains flying and
 * shroud until end of turn.
 */
val WarpedResearcher = card("Warped Researcher") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Human Wizard Mutant"
    power = 3
    toughness = 4
    oracleText = "Whenever a player cycles a card, Warped Researcher gains flying and shroud until end of turn. (It can't be the target of spells or abilities.)"

    triggeredAbility {
        trigger = Triggers.AnyPlayerCycles
        effect = CompositeEffect(listOf(
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.SHROUD, EffectTarget.Self)
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "rk post"
        flavorText = "New insights yield new senses."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5df94a4e-1371-4b75-a557-eeb83c23cf9d.jpg?1562914038"
    }
}
