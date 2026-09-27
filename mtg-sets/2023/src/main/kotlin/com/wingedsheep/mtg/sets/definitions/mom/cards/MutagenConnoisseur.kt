package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mutagen Connoisseur — March of the Machine #248.
 * {1}{G}{U} · Creature — Vedalken Mutant · 0/5
 *
 * Flying, vigilance
 * This creature gets +1/+0 for each transformed permanent you control.
 *
 * "Transformed permanent" is CR 701.27g's term — a double-faced permanent with its back face up —
 * read by `transformed()` (`StatePredicate.IsTransformed`), so a DFC sitting on its front face and
 * a modal double-faced permanent on either face add nothing.
 */
val MutagenConnoisseur = card("Mutagen Connoisseur") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Vedalken Mutant"
    power = 0
    toughness = 5
    oracleText = "Flying, vigilance\n" +
        "This creature gets +1/+0 for each transformed permanent you control."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.transformed()).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Alex Brock"
        flavorText = "Much to the Phyrexians' delight, sheer curiosity drove many to willingly " +
            "surrender to *compleation*."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a84b6d7-3944-4223-ac16-aa5e59ac84cb.jpg?1783916941"
        ruling("2023-04-14", "A \"transformed permanent\" is a double-faced permanent with its back face up. Notably, modal double-faced permanents and melded permanents are never transformed permanents, no matter which faces are up.")
    }
}
