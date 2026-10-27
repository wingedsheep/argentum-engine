package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * King Harald's Revenge
 * {2}{G}
 * Sorcery
 * Until end of turn, target creature gets +1/+1 for each creature you control and gains trample.
 * It must be blocked this turn if able.
 *
 * The bonus counts the creatures you control as the spell resolves and is locked in for the turn
 * (the Junkyo Bell shape). "Must be blocked if able" needs only one blocker, so the floating
 * requirement is [Effects.MustBeBlocked] with `allCreatures = false` (the Vengeant Earth shape).
 */
val KingHaraldsRevenge = card("King Harald's Revenge") {
    manaCost = "{2}{G}"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, target creature gets +1/+1 for each creature you control and gains " +
        "trample. It must be blocked this turn if able."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            DynamicAmounts.creaturesYouControl(),
            DynamicAmounts.creaturesYouControl(),
            creature,
        ) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.MustBeBlocked(creature, allCreatures = false)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"We will destroy the pretenders and reclaim the divinity of the Einir.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71a659b6-c4a8-4c0a-8b2a-07b6c9e27dfc.jpg?1783928210"
        ruling(
            "2021-02-05",
            "Use the number of creatures you control as King Harald's Revenge resolves to determine how big " +
                "the power and toughness boost is.",
        )
        ruling(
            "2021-02-05",
            "If the creature attacks, the defending player must assign at least one blocker to it if that " +
                "player controls any creatures that could block it. Other creatures may also block it, block " +
                "other creatures, or not block at all.",
        )
    }
}
