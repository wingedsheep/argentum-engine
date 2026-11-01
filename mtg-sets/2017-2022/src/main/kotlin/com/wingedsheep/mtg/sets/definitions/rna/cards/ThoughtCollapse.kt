package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thought Collapse — Ravnica Allegiance #57
 * {1}{U}{U} · Instant
 *
 * Counter target spell. Its controller mills three cards.
 *
 * Same shape as Didn't Say Please: the mill reads the spell's controller through
 * `Player.ControllerOf`, which still answers once the spell has been countered — and when the spell
 * can't be countered and stays on the stack (ruling).
 */
val ThoughtCollapse = card("Thought Collapse") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. Its controller mills three cards."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell() then
            Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.ControllerOf("target spell")))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Sara Winters"
        flavorText = "\"I can think of no greater punishment than answering your question in full.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/948b569b-6341-418b-99b5-f79dfb3fe8dd.jpg?1783933703"
        ruling(
            "2019-01-25",
            "A spell that can't be countered is a legal target for Thought Collapse. The spell won't be " +
                "countered when Thought Collapse resolves, but its controller will still put the top three " +
                "cards of their library into their graveyard."
        )
    }
}
