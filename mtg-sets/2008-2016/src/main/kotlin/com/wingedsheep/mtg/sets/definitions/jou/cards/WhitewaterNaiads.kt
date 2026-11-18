package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Whitewater Naiads
 * {3}{U}{U}
 * Enchantment Creature — Nymph
 * 4/4
 *
 * Constellation — Whenever this creature or another enchantment you control enters, target creature can't be blocked this turn.
 */
val WhitewaterNaiads = card("Whitewater Naiads") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Nymph"
    power = 4
    toughness = 4
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, target creature can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "target creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "William Wu"
        flavorText = "Stand against the crash of the river's rapids and you will know the power of the naiads."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f61c85c-03cf-411a-aa66-cbe178aeaa02.jpg?1783939440"
    }
}
