package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Slickshot Vault-Buster — Outlaws of Thunder Junction #68
 * {2}{U} · Creature — Human Rogue · Common
 * 1/4
 *
 * Vigilance
 * This creature gets +2/+0 as long as you've committed a crime this turn.
 *
 * The buff is a [ConditionalStaticAbility] gated on the turn-scoped crime tracker
 * ([Conditions.YouCommittedCrimeThisTurn]) — once any crime is committed it stays a 3/4 for
 * the rest of the turn, then reverts at cleanup when the tracker resets.
 */
val SlickshotVaultBuster = card("Slickshot Vault-Buster") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 4
    oracleText = "Vigilance\n" +
        "This creature gets +2/+0 as long as you've committed a crime this turn. (Targeting " +
        "opponents, anything they control, and/or cards in their graveyards is a crime.)"

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 0, Filters.Self),
            condition = Conditions.YouCommittedCrimeThisTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Julia Metzger"
        flavorText = "\"Sure it's risky, but that's what makes it fun! That and the money. " +
            "And the bankers' faces.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/592ccc36-3d10-4a12-8743-9b300b80cb4d.jpg?1712355501"
    }
}
