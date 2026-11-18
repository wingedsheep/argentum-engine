package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Countermand — Journey into Nyx #33
 * {2}{U}{U} · Instant
 *
 * Counter target spell. Its controller mills four cards.
 *
 * Same shape as Didn't Say Please: the mill reads the spell's controller through
 * `Player.ControllerOf`, which still answers once the spell has been countered — and when the spell
 * can't be countered and stays on the stack (ruling).
 */
val Countermand = card("Countermand") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. Its controller mills four cards."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell() then
            Patterns.Library.mill(4, EffectTarget.PlayerRef(Player.ControllerOf("target spell")))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Clint Cearley"
        flavorText = "The physical pain was nothing compared to the agony in his mind."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07815e32-0b64-4c2b-84e6-a72336c45cf5.jpg?1783939449"
        ruling(
            "2014-04-26",
            "You may target a spell that can't be countered with Countermand. If you do, when Countermand " +
                "resolves, the target spell won't be countered, but its controller will still put the top " +
                "four cards of their library into their graveyard."
        )
    }
}
