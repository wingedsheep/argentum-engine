package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dutiful Attendant
 * {2}{B}
 * Creature — Human Warrior
 * 1/2
 *
 * When this creature dies, return another target creature card from your graveyard to your hand.
 *
 * The target is chosen as the dies trigger goes on the stack, when Dutiful Attendant is already in
 * the graveyard — `.other()` keeps it from returning itself ("another").
 */
val DutifulAttendant = card("Dutiful Attendant") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 2
    oracleText = "When this creature dies, return another target creature card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(TargetFilter.CreatureInYourGraveyard.other())
        effect = Effects.ReturnToHand(creature)
        description = "When this creature dies, return another target creature card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Aaron Miller"
        flavorText = "The dragon who eats the last head in the basket is entitled to the servant's."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ee71b19-eb1d-4f5a-a1b7-1799f2f50da0.jpg?1783938598"
    }
}
