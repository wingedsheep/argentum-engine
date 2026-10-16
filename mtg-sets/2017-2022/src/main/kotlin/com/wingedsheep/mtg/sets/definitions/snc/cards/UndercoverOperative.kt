package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CopyEntryCounters
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter


/**
 * Undercover Operative — Streets of New Capenna #63.
 *
 * A Clone whose shield counter rides on the copy effect (`EntersAsCopy.entersWithCounters`), gated
 * on the copied creature by `ifCopiedMatches = youControl()`: copying an opponent's creature, or
 * declining the copy, enters without a shield counter.
 */
val UndercoverOperative = card("Undercover Operative") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter Rogue"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield, except it " +
        "enters with a shield counter on it if you control that creature. (If it would be dealt damage or " +
        "destroyed, remove a shield counter from it instead.)"

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature,
            entersWithCounters = CopyEntryCounters(
                count = DynamicAmounts.fixed(1),
                counterType = CounterType.SHIELD,
                ifCopiedMatches = GameObjectFilter.Any.youControl(),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Tyler Walpole"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ecec953-ab86-409b-b618-04bb07ff3eac.jpg?1783923139"
    }
}
