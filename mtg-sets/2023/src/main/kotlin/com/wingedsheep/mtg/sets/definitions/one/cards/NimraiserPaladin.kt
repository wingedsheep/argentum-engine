package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nimraiser Paladin
 * {4}{B}
 * Creature — Phyrexian Knight
 * 4/4
 *
 * Toxic 2 (Players dealt combat damage by this creature also get two poison counters.)
 * When this creature enters, return target creature card with mana value 3 or less from your graveyard to your hand.
 */
val NimraiserPaladin = card("Nimraiser Paladin") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Knight"
    power = 4
    toughness = 4
    oracleText = "Toxic 2 (Players dealt combat damage by this creature also get two poison counters.)\n" +
        "When this creature enters, return target creature card with mana value 3 or less from your graveyard to your hand."

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 2))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val returned = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(3))
        effect = Effects.ReturnToHand(returned)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a99fe9a8-d9e7-4286-81a1-adfb753e4741.jpg?1783918043"
    }
}
