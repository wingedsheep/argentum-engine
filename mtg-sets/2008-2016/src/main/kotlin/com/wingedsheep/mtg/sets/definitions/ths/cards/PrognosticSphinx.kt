package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prognostic Sphinx
 * {3}{U}{U}
 * Creature — Sphinx
 * 3/5
 * Flying
 * Discard a card: This creature gains hexproof until end of turn. Tap it.
 * Whenever this creature attacks, scry 3.
 *
 * The hexproof-then-tap activation is the Homarid Warrior / Deep Spawn shape with a discard cost.
 */
val PrognosticSphinx = card("Prognostic Sphinx") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    oracleText = "Flying\nDiscard a card: This creature gains hexproof until end of turn. Tap it.\n" +
        "Whenever this creature attacks, scry 3."
    power = 3
    toughness = 5

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self) then
            Effects.Tap(EffectTarget.Self)
        description = "Discard a card: This creature gains hexproof until end of turn. Tap it."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Library.scry(3)
        description = "Whenever this creature attacks, scry 3."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f0a27a6-54b8-4615-89b3-591a4e3ef626.jpg?1783939793"
    }
}
