package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Faerie Snoop — Murders at Karlov Manor #203
 * {1}{U}{B} · Creature — Faerie Detective · 1/4
 *
 * Flying
 * Disguise {1}{U/B}{U/B}
 * When this creature is turned face up, look at the top two cards of your library. Put one into your
 * hand and the other into your graveyard.
 *
 * Face down it is a colorless, typeless 2/2 with ward {2} (CR 708.2) — no flying, no Detective type,
 * no trigger — so the Snoop only pays off through the disguise route. Turning face up is a special
 * action, not entering the battlefield (CR 701.34c), which is why this is a `TurnedFaceUp` trigger and
 * not an enters trigger.
 *
 * The look is *not* impulse card selection: both cards leave the top, one to hand and one to the
 * graveyard, so `lookAtTopAndKeep(count = 2, keepCount = 1)` with its default HAND / GRAVEYARD
 * destinations is the literal reading. `ChooseExactly(1)` is right rather than "up to" — the oracle
 * text has no "may", and with a one-card library the selection just runs over whatever was gathered.
 *
 * The cards are looked at, not revealed, so the graveyard half is only public once it lands there.
 */
val FaerieSnoop = card("Faerie Snoop") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Faerie Detective"
    oracleText = "Flying\n" +
        "Disguise {1}{U/B}{U/B} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, look at the top two cards of your library. Put one " +
        "into your hand and the other into your graveyard."
    power = 1
    toughness = 4

    keywords(Keyword.FLYING)
    disguise = "{1}{U/B}{U/B}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Patterns.Library.lookAtTopAndKeep(count = 2, keepCount = 1)
        description = "When this creature is turned face up, look at the top two cards of your " +
            "library. Put one into your hand and the other into your graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Dallas Williams"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20267dab-8898-4b44-8ef4-8a239967662c.jpg?1783912849"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "The resulting creature is a 2/2 creature with ward {2} that has no name, mana cost, or " +
                "creature types. Other effects that apply to the creature can still grant it any " +
                "characteristics it doesn't have."
        )
    }
}
