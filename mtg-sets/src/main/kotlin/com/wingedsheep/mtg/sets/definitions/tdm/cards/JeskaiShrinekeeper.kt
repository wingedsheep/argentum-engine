package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jeskai Shrinekeeper — Tarkir: Dragonstorm #197
 * {2}{U}{R}{W} · Creature — Dragon · 3/3
 *
 * Flying, haste
 * Whenever this creature deals combat damage to a player, you gain 1 life and draw a card.
 *
 * Composes the two combat-damage payoffs with [Effects.Composite] over [Effects.GainLife] and
 * [Effects.DrawCards], both controller-scoped. The trigger reuses [Triggers.DealsCombatDamageToPlayer].
 */
val JeskaiShrinekeeper = card("Jeskai Shrinekeeper") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 3
    oracleText = "Flying, haste\n" +
        "Whenever this creature deals combat damage to a player, you gain 1 life and draw a card."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Composite(
            Effects.GainLife(1),
            Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ec8fa0b-c695-4326-aebd-042cb1974925.jpg?1743204771"
    }
}
