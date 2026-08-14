package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Sudden Setback — Murders at Karlov Manor #72
 * {2}{U}{U} · Instant · Uncommon
 *
 * The owner of target spell or nonland permanent puts it on their choice of the top or bottom of
 * their library.
 *
 * A counterspell and a removal spell in one card, and neither half is a counter: a spell answered
 * this way is *removed from the stack* rather than countered, so "can't be countered" doesn't save
 * it. [TargetSpellOrPermanent] is the union target that already models "target spell or nonland
 * permanent" (Press the Enemy), with `permanentFilter = NonlandPermanent` — unrestricted by
 * controller, so it can also rescue your own creature from removal by tucking it.
 *
 * The library placement is [Effects.PutOnTopOrBottomOfLibrary], whose executor dispatches on what
 * the target resolves to (stack object vs. battlefield permanent) and pauses for a
 * ChooseOptionDecision. Two details of that decision match the printed card and are easy to get
 * wrong: the choice belongs to the **owner**, not to Sudden Setback's controller, and it is made on
 * resolution rather than on cast.
 */
val SuddenSetback = card("Sudden Setback") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "The owner of target spell or nonland permanent puts it on their choice of the " +
        "top or bottom of their library."

    spell {
        val t = target(
            "target spell or nonland permanent",
            TargetSpellOrPermanent(permanentFilter = GameObjectFilter.NonlandPermanent)
        )
        effect = Effects.PutOnTopOrBottomOfLibrary(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Olivier Bernard"
        flavorText = "\"Look, buddy, you're not in trouble. I just wanna ask you a questio—\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b9e5fd6-a5ea-4ae5-83f5-89ed6a658dd3.jpg?1783912904"

        ruling(
            "2024-02-02",
            "The spell or permanent's owner chooses whether to put it on the top or bottom of " +
                "their library. If multiple cards are put into the library this way (such as when " +
                "the spell targets a melded permanent), that spell or permanent's owner puts all " +
                "the cards on top or all the cards on the bottom. They put them in whatever order " +
                "they wish, and they do not need to reveal the order."
        )
    }
}
