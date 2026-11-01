package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Samut, Tyrant Smasher
 * {2}{R/G}{R/G}
 * Legendary Planeswalker — Samut
 * Loyalty: 5
 *
 * Creatures you control have haste.
 * −1: Target creature gets +2/+1 and gains haste until end of turn. Scry 1.
 *
 * The Fire Crystal's group haste grant, plus Samut's Sprint's exact text as the −1.
 */
val SamutTyrantSmasher = card("Samut, Tyrant Smasher") {
    manaCost = "{2}{R/G}{R/G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Samut"
    startingLoyalty = 5
    oracleText = "Creatures you control have haste.\n" +
        "−1: Target creature gets +2/+1 and gains haste until end of turn. Scry 1."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
        )
    }

    loyaltyAbility(-1) {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 1, t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.Scry(1)
        description = "Target creature gets +2/+1 and gains haste until end of turn. Scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0457e3f-56c1-401b-8e08-f04b95c55e2d.jpg?1783933374"
    }
}
