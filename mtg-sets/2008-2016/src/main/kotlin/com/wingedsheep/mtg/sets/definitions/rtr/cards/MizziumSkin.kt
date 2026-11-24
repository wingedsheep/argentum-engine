package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mizzium Skin — Return to Ravnica #45 (canonical printing)
 * {U} · Instant
 *
 * Target creature you control gets +0/+1 and gains hexproof until end of turn.
 * Overload {1}{U}
 *
 * Dynacharge's overload shape, with Overrun's one-pass pump-and-grant for the overloaded mode.
 */
val MizziumSkin = card("Mizzium Skin") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +0/+1 and gains hexproof until end of turn.\n" +
        "Overload {1}{U} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{1}{U}"))

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(0, 1, creature) then Effects.GrantKeyword(Keyword.HEXPROOF, creature)

        overloadEffect = Patterns.Group.pumpAndGrantToAll(0, 1, Keyword.HEXPROOF, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Scott Chou"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9859344-4efc-4b87-a3fb-147e496cee68.jpg?1783940367"
        ruling(
            "2024-01-12",
            "If you don't pay the overload cost of a spell with overload, that spell will have a single " +
                "target. If you pay the overload cost, the spell won't have any targets."
        )
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
        ruling(
            "2024-01-12",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying (such as an overload cost), add any cost increases, then apply any cost reductions. " +
                "The mana value of the spell remains unchanged, no matter what the total cost to cast it was."
        )
        ruling(
            "2024-01-12",
            "If you are instructed to cast a spell with overload \"without paying its mana cost,\" you can't " +
                "choose to pay its overload cost instead."
        )
    }
}
