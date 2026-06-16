package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.opus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elemental Mascot
 * {1}{U}{R}
 * Creature — Elemental Bird
 * 1/4
 *
 * Flying, vigilance
 * Opus — Whenever you cast an instant or sorcery spell, this creature gets +1/+0 until end of turn.
 * If five or more mana was spent to cast that spell, exile the top card of your library. You may
 * play that card until the end of your next turn.
 *
 * "Opus" is an ability word (flavor only). The `opus { }` builder wires the spell-cast trigger and
 * the 5+ mana tier. The base effect is +1/+0; the `alsoIfFiveOrMore` bonus is the standard impulse
 * mechanic (`Patterns.Exile.impulse`) with `MayPlayExpiry.UntilEndOfNextTurn`.
 */
val ElementalMascot = card("Elemental Mascot") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Elemental Bird"
    power = 1
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "Opus — Whenever you cast an instant or sorcery spell, this creature gets +1/+0 until " +
        "end of turn. If five or more mana was spent to cast that spell, exile the top card of " +
        "your library. You may play that card until the end of your next turn."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    opus {
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        alsoIfFiveOrMore = Patterns.Exile.impulse(
            count = 1,
            expiry = MayPlayExpiry.UntilEndOfNextTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Justyna Dura"
        flavorText = "It soars in the presence of true beauty."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c507eb1c-48e9-4d28-bb2d-71f2a9df9ab0.jpg?1775938280"
    }
}
