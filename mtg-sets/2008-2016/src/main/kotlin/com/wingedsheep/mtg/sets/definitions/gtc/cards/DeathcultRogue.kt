package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Deathcult Rogue
 * {1}{U/B}{U/B}
 * Creature — Human Rogue
 * 2/2
 * This creature can't be blocked except by Rogues.
 */
val DeathcultRogue = card("Deathcult Rogue") {
    manaCost = "{1}{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    oracleText = "This creature can't be blocked except by Rogues."
    power = 2
    toughness = 2

    staticAbility {
        ability = CantBeBlockedExceptBy(GameObjectFilter.Permanent.withSubtype(Subtype.ROGUE))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "218"
        artist = "David Palumbo"
        flavorText = "\"The hussars can't catch him, and he evades our traps with ease. I fear he walks unseen even in the halls of New Prahv.\"\n—Arrester Lavinia, Tenth Precinct"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4c186d2-e631-4811-83ea-fdb54e730a5d.jpg"
    }
}
