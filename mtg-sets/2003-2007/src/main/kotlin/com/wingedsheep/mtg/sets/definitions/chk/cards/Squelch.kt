package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Squelch
 * {1}{U}
 * Instant
 *
 * Counter target activated ability. (Mana abilities can't be targeted.)
 * Draw a card.
 *
 * The blue twin of Bind: a [TargetFilter.ActivatedAbilityOnStack] target (which names
 * `CardPredicate.IsActivatedAbility`, so only activated abilities on the stack are offered —
 * mana abilities never use the stack) followed by a draw.
 */
val Squelch = card("Squelch") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target activated ability. (Mana abilities can't be targeted.)\nDraw a card."

    spell {
        val activatedAbility = target(TargetFilter.ActivatedAbilityOnStack)
        effect = Effects.CounterAbility() then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Matt Cavotta"
        flavorText = "Oku-Doku had gone through all the motions: the same akki cursewords, the same ingredients with their horrid stink, the same rude gestures. Yet not a person died."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29421dd2-70a7-4623-afe0-ca4cb415ec87.jpg?1783944321"
        ruling("2004-12-01", "Squelch can't target a mana ability because mana abilities don't use the stack.")
    }
}
