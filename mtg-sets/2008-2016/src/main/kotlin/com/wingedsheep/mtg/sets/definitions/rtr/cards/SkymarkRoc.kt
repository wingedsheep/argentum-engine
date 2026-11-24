package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skymark Roc
 * {2}{W}{U}
 * Creature — Bird
 * 3/3
 * Flying
 * Whenever this creature attacks, you may return target creature defending player controls with
 * toughness 2 or less to its owner's hand.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * The target is scoped with `defendingPlayerControls()` (as on Blaze of Glory); [Effects.May]
 * around the bounce is the "you may", as on Mistblade Shinobi.
 */
val SkymarkRoc = card("Skymark Roc") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature attacks, you may return target creature defending player controls " +
        "with toughness 2 or less to its owner's hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.defendingPlayerControls().toughnessAtMost(2))
        effect = Effects.May(Effects.ReturnToHand(creature))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Christopher Moeller"
        flavorText = "\"Try not to look guilty. Or delicious.\"\n—Thorpe, street urchin"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60601296-2229-4c48-94cc-1903926750ce.jpg?1783940331"
    }
}
