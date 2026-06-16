package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sheriff of Safe Passage
 * {2}{W}
 * Creature — Human Knight
 * 0/0
 * This creature enters with a +1/+1 counter on it plus an additional +1/+1 counter on it for
 * each other creature you control.
 * Plot {1}{W}
 *
 * "Each other creature you control" is naturally captured by counting the controller's creatures
 * during the enters-with-counters replacement (the Sheriff is not yet on the battlefield when the
 * count is evaluated — same evaluation timing as Stag Beetle's "other creatures on the
 * battlefield").
 */
val SheriffOfSafePassage = card("Sheriff of Safe Passage") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "This creature enters with a +1/+1 counter on it plus an additional +1/+1 counter on it for each other creature you control.\nPlot {1}{W} (You may pay {1}{W} and exile this card from your hand. Cast it as a sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"
    power = 0
    toughness = 0

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.Add(
                DynamicAmount.Fixed(1),
                DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature)
            )
        )
    )

    keywordAbility(KeywordAbility.plot("{1}{W}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c38a845d-f25d-45ab-9154-3fa5291b0ba0.jpg?1712355345"
    }
}
