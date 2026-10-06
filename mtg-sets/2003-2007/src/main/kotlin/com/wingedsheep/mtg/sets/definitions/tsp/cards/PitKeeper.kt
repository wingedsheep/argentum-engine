package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pit Keeper
 * {1}{B}
 * Creature — Human Wizard
 * 2/1
 * When this creature enters, if you have four or more creature cards in your graveyard, you may
 * return target creature card from your graveyard to your hand.
 *
 * The same intervening-if shape as Oversold Cemetery, on an entry trigger.
 */
val PitKeeper = card("Pit Keeper") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, if you have four or more creature cards in your graveyard, you may return target creature card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        interveningIf = Conditions.CreatureCardsInGraveyardAtLeast(4)
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(target = t, destination = Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Anthony S. Waters"
        flavorText = "\"The undead are not 'awakened.' They are evicted when there is no room left in the pit for more bodies.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8936e767-0e48-4adb-93e9-790fe0cc19f2.jpg?1783943230"
    }
}
