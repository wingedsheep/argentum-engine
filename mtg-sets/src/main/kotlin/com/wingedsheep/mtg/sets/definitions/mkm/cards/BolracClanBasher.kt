package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bolrac-Clan Basher — Murders at Karlov Manor #112
 * {4}{R}{R} · Creature — Cyclops Warrior · 3/2
 *
 * Double strike, trample
 * Disguise {3}{R}{R}
 *
 * A pure rate card: six mana for a 3/2 is unplayable, so the disguise line *is* the card. Down for
 * {3} on turn three it is an anonymous 2/2 with ward {2} (CR 702.168a) — the double strike and
 * trample are suppressed with the rest of its abilities (CR 708.2) — and the {3}{R}{R} flip can be
 * paid mid-combat, after blocks, turning a chump block into six trampling damage.
 *
 * Flipping is a special action that doesn't use the stack (CR 701.34a), so there is no window for
 * the defender to respond once the attack is already committed.
 */
val BolracClanBasher = card("Bolrac-Clan Basher") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cyclops Warrior"
    oracleText = "Double strike, trample\n" +
        "Disguise {3}{R}{R} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)"
    power = 3
    toughness = 2
    keywords(Keyword.DOUBLE_STRIKE, Keyword.TRAMPLE)
    disguise = "{3}{R}{R}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Warren Mahy"
        flavorText = "Determining the culprit was not difficult. Getting him to stop committing " +
            "the crime long enough to be arrested, however, was a massive challenge."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b87683f7-8a61-4e4a-8b8b-3bf812454096.jpg?1783912887"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "Turning a permanent face up or face down doesn't change whether that permanent is " +
                "tapped or untapped."
        )
    }
}
