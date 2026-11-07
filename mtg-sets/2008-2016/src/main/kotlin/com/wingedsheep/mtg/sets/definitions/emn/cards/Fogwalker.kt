package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fogwalker
 * {1}{U}
 * Creature — Spirit
 * 1/3
 *
 * Skulk (This creature can't be blocked by creatures with greater power.)
 * When this creature enters, target creature an opponent controls doesn't untap during its
 * controller's next untap step.
 */
val Fogwalker = card("Fogwalker") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 3
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "When this creature enters, target creature an opponent controls doesn't untap during its controller's next untap step."

    keywords(Keyword.SKULK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            creature,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0894d76b-801a-4e3b-8378-bfa81fe24d59.jpg?1783937499"
    }
}
