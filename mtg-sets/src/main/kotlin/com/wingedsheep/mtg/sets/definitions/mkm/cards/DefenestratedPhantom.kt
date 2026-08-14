package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Defenestrated Phantom — Murders at Karlov Manor #11
 * {4}{W}{W} · Creature — Spirit · 4/3
 *
 * Flying
 * Disguise {4}{W}
 *
 * A pure vanilla-plus-disguise body: the whole card is two keywords, so there is no script block at
 * all. Face down it is the standard 2/2 with ward {2} and no abilities (CR 702.168a / 708.2) — it
 * doesn't fly until it's turned face up. Note the disguise cost {4}{W} is a *worse* rate than just
 * hard-casting it for {4}{W}{W}, so disguise here is about timing (a surprise blocker that flips
 * mid-combat) rather than about cheating on mana.
 */
val DefenestratedPhantom = card("Defenestrated Phantom") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    oracleText = "Flying\n" +
        "Disguise {4}{W} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)"
    power = 4
    toughness = 3
    keywords(Keyword.FLYING)
    disguise = "{4}{W}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Svetlin Velinov"
        flavorText = "\"Never parley near high windows.\"\n—Teysa"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b284304-c3bf-4413-9fd3-b44eb4eb642a.jpg?1783912926"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "The resulting creature is a 2/2 creature with ward {2} that has no name, mana cost, " +
                "or creature types. Both the spell and the resulting creature are colorless and " +
                "have a mana value of 0."
        )
    }
}
