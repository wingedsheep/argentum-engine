package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Saving Grasp
 * {U}
 * Instant
 * Return target creature you own to your hand.
 * Flashback {W}
 *
 * "You own" restricts the target by owner, not controller — a creature you own that an opponent
 * controls is a legal target, and it returns to its owner's (your) hand.
 */
val SavingGrasp = card("Saving Grasp") {
    manaCost = "{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Return target creature you own to your hand.\n" +
        "Flashback {W} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val creature = target(TargetFilter.Creature.ownedByYou())
        effect = Effects.ReturnToHand(creature)
    }
    keywordAbility(KeywordAbility.flashback("{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Matt Stewart"
        flavorText = "Nothing's more valuable than perfect timing."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/914837df-c255-4cea-9255-b05f218fd9f8.jpg?1783940837"
    }
}
