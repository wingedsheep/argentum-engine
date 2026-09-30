package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedLoyaltyAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ichormoon Gauntlet
 * {2}{U}
 * Artifact
 * Planeswalkers you control have "[0]: Proliferate" and "[−12]: Take an extra turn after this one."
 * Whenever you cast a noncreature spell, choose a counter on target permanent. Put an additional
 * counter of that kind on that permanent.
 */
val IchormoonGauntlet = card("Ichormoon Gauntlet") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "Planeswalkers you control have \"[0]: Proliferate\" and \"[−12]: Take an extra turn after this one.\"\n" +
        "Whenever you cast a noncreature spell, choose a counter on target permanent. Put an additional counter of that kind on that permanent."

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedLoyaltyAbility(0) {
                effect = Effects.Proliferate()
                description = "Proliferate."
            },
            filter = GroupFilter(GameObjectFilter.Planeswalker.youControl())
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedLoyaltyAbility(-12) {
                effect = Effects.TakeExtraTurn()
                description = "Take an extra turn after this one."
            },
            filter = GroupFilter(GameObjectFilter.Planeswalker.youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.AddCountersOfChosenKind(permanent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "56"
        artist = "Tiffany Turrill"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c093ce0-2561-4847-ab58-f6f1650d743e.jpg?1786522813"
    }
}
