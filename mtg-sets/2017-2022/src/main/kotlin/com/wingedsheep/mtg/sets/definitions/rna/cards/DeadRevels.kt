package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dead Revels
 * {3}{B}
 * Sorcery
 * Spectacle {1}{B} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 * Return up to two target creature cards from your graveyard to your hand.
 */
val DeadRevels = card("Dead Revels") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Spectacle {1}{B} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "Return up to two target creature cards from your graveyard to your hand."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{1}{B}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "David Palumbo"
        flavorText = "You're only as dead as you feel."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5b2c21a-419d-4896-82e3-1b5cb32b158e.jpg?1783933694"
    }
}
