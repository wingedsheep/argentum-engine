package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merfolk Cave-Diver
 * {2}{U}
 * Creature — Merfolk Scout
 * 2/4
 *
 * Whenever a creature you control explores, this creature gets +1/+0 until end of turn and can't
 * be blocked this turn.
 *
 * Uses the [Triggers.WheneverCreatureYouControlExplores] battlefield trigger (CR 701.44). The
 * payoff pumps and evades the source itself ([EffectTarget.Self]) until end of turn (both
 * [Effects.ModifyStats] and [GrantKeywordEffect] default to [com.wingedsheep.sdk.scripting.Duration.EndOfTurn]).
 */
val MerfolkCaveDiver = card("Merfolk Cave-Diver") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Scout"
    oracleText = "Whenever a creature you control explores, this creature gets +1/+0 until end of " +
        "turn and can't be blocked this turn."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.WheneverCreatureYouControlExplores
        effect = Effects.Composite(
            Effects.ModifyStats(1, 0, EffectTarget.Self),
            GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Fesbra"
        flavorText = "\"Learn all you can of this underground sea. I suspect more secrets of our " +
            "ancestors lie waiting in its depths.\"\n—Tishana"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e0fe81c-b8ef-49ff-8743-f03d0220cb9e.jpg?1782694558"
    }
}
