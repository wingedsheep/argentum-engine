package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.opus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Expressive Firedancer
 * {1}{R}
 * Creature — Human Sorcerer
 * 2/2
 * Opus — Whenever you cast an instant or sorcery spell, this creature gets +1/+1 until end of
 * turn. If five or more mana was spent to cast that spell, this creature also gains double
 * strike until end of turn.
 *
 * "Opus" is an ability word (flavor only). The `opus { }` builder wires the spell-cast trigger and
 * the 5+ mana tier (`ContextProperty(MANA_SPENT_ON_TRIGGERING_SPELL) >= 5`). The double strike is
 * `alsoIfFiveOrMore`, so it runs in addition to the unconditional +1/+1.
 */
val ExpressiveFiredancer = card("Expressive Firedancer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Sorcerer"
    power = 2
    toughness = 2
    oracleText = "Opus — Whenever you cast an instant or sorcery spell, this creature gets " +
        "+1/+1 until end of turn. If five or more mana was spent to cast that spell, this " +
        "creature also gains double strike until end of turn."

    opus {
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        alsoIfFiveOrMore = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Billy Christian"
        flavorText = "\"Now, let your movements respond to the music! Yes, just like that!\"\n" +
            "—Introduction to Interpretation"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/259b8c45-6241-4206-a34e-34c7f401f47b.jpg?1775937734"
    }
}
