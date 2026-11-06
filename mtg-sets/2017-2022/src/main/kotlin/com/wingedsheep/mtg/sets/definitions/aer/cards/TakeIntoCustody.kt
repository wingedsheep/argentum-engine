package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Take into Custody
 * {U}
 * Instant
 * Tap target creature. It doesn't untap during its controller's next untap step.
 */
val TakeIntoCustody = card("Take into Custody") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap target creature. It doesn't untap during its controller's next untap step."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, creature, Duration.UntilAfterAffectedControllersNextUntap())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "David Palumbo"
        flavorText = "\"You can tell me what I want to know, or you can talk to the Chief of Compliance. Why don't you make it easy on yourself?\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b412718a-7bc7-4b16-af60-1b955c820b0f.jpg"
        ruling("2017-02-09", "Take into Custody can target a creature that’s already tapped. That creature won’t untap during its controller’s next untap step.")
    }
}
