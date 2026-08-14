package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Queen of Dale
 * {1}{W}
 * Legendary Creature — Human Noble
 * 2/1
 *
 * Whenever an opponent casts their first noncreature spell each turn, you recruit.
 *
 * The ordinal is per-kind — an opponent who leads with two creature spells and then a noncreature
 * one still hands you a recruit, because the count runs over that opponent's *noncreature* casts
 * only. That's the `spellFilter` on [Triggers.NthSpellCast], counted off the caster's cast history,
 * so it counts casts rather than resolutions: a countered noncreature spell still burns the
 * opponent's window for the turn.
 *
 * "Each turn" is per turn and per opponent, not per turn total — the count is keyed to the caster,
 * so in a multiplayer game each opponent's own first noncreature spell triggers separately.
 */
val TheQueenOfDale = card("The Queen of Dale") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Noble"
    oracleText = "Whenever an opponent casts their first noncreature spell each turn, you recruit. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, create a 1/1 white " +
        "Human Soldier creature token.)"
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.NthSpellCast(
            n = 1,
            player = Player.EachOpponent,
            spellFilter = GameObjectFilter.Noncreature
        )
        effect = Patterns.Mechanic.recruit()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "24"
        artist = "Magali Villeneuve"
        flavorText = "Dale may grow to reclaim its former glory and splendor, as in the days " +
            "before the Dragon came."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c977fb5f-4436-41d0-af68-93b6d05897e5.jpg?1784376948"
    }
}
