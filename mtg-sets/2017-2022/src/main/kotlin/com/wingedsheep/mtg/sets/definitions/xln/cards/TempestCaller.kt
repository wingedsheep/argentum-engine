package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tempest Caller
 * {2}{U}{U}
 * Creature — Merfolk Wizard
 * 2/3
 * When this creature enters, tap all creatures target opponent controls.
 *
 * The enters trigger targets an opponent and taps each creature that player controls on
 * resolution — [Patterns.Group.tapAll] over `targetOpponentControls()`, the targeted-opponent
 * group shape of [com.wingedsheep.mtg.sets.definitions.vis.cards.Simoon].
 */
val TempestCaller = card("Tempest Caller") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, tap all creatures target opponent controls."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(Targets.Opponent)
        effect = Patterns.Group.tapAll(GroupFilter(Filters.Creature.targetOpponentControls()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Ryan Alexander Lee"
        flavorText = "\"I *am* the storm.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64d0f22d-0448-4039-b6f3-75d12e10f48c.jpg?1783935768"
    }
}
