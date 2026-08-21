package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sythis, Harvest's Hand — Modern Horizons 2 #214
 * {G}{W} · Legendary Enchantment Creature — Nymph · 1 / 2
 *
 * Whenever you cast an enchantment spell, you gain 1 life and draw a card.
 *
 * [Triggers.YouCastEnchantment] watches the *cast*, so the trigger goes on the stack above the
 * enchantment spell and resolves first — the life and the card arrive before the enchantment does
 * (CR 603.3). Sythis is herself an enchantment creature, but casting *her* does not trigger this:
 * the ability isn't on the battlefield yet when she is cast.
 *
 * "You gain 1 life and draw a card" is one instruction sequence, so it is a single composite
 * effect (`then`) rather than two abilities — a second `triggeredAbility` would put two objects on
 * the stack and let a player respond between the halves.
 */
val SythisHarvestsHand = card("Sythis, Harvest's Hand") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Enchantment Creature — Nymph"
    power = 1
    toughness = 2
    oracleText = "Whenever you cast an enchantment spell, you gain 1 life and draw a card."

    triggeredAbility {
        trigger = Triggers.YouCastEnchantment
        effect = Effects.GainLife(1) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Ryan Yee"
        flavorText = "\"Through every verdant field, every bough that hangs heavy with fruit, Karametra shows her love.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0babfe00-9bad-48fc-b3b1-df8280242fd2.jpg?1783926809"
    }
}
