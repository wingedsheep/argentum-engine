package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Contradict — Dragons of Tarkir #49 (canonical printing)
 * {3}{U}{U} · Instant
 *
 * Counter target spell.
 * Draw a card.
 */
val Contradict = card("Contradict") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell.\nDraw a card."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell() then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Steve Prescott"
        flavorText = "Those who question Ojutai may not like the answers they receive."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0b3d4ff-09d1-4d9f-8c83-cdfbd7bb1079.jpg?1783938611"
        ruling("2015-02-25", "If the spell becomes an illegal target (because it’s left the stack, for example) when Contradict tries to resolve, Contradict won’t resolve and none of its effects will happen. You won’t draw a card.")
        ruling("2015-02-25", "A spell that can’t be countered by spells and abilities is a legal target for Contradict. The spell won’t be countered when Contradict resolves, but you’ll draw a card.")
    }
}
