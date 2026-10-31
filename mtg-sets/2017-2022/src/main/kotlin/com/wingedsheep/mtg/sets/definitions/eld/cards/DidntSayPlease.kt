package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Didn't Say Please — Throne of Eldraine #42 (canonical printing)
 * {1}{U}{U} · Instant
 *
 * Counter target spell. Its controller mills three cards.
 *
 * Induce Paranoia's shape without the rider: the mill reads the spell's controller through
 * `Player.ControllerOf`, which still answers once the spell is in the graveyard — and also when the
 * spell can't be countered and stays on the stack, where its controller still mills (ruling).
 */
val DidntSayPlease = card("Didn't Say Please") {
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
        collectorNumber = "42"
        artist = "Jason A. Engle"
        flavorText = "\"If you're in our home, we expect you to mind your manners.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77500b53-0852-4d6a-bfe3-b1e8ef5a12cd.jpg?1783932660"
        ruling(
            "2019-10-04",
            "A spell that can't be countered is a legal target for Didn't Say Please. The spell won't be " +
                "countered when Didn't Say Please resolves, but its controller still puts the top three " +
                "cards of their library into their graveyard."
        )
    }
}
