package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Virulent Plague
 * {2}{B}
 * Enchantment
 * Creature tokens get -2/-2.
 *
 * Applies to every creature token on the battlefield, under any player's control.
 */
val VirulentPlague = card("Virulent Plague") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Creature tokens get -2/-2."

    staticAbility {
        ability = ModifyStats(
            powerBonus = -2,
            toughnessBonus = -2,
            filter = GroupFilter(GameObjectFilter.Creature.token())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Johann Bodin"
        flavorText = "\"This pestilence robs us of glorious death in battle. We starve to death with full bellies and drown trying to slake our unnatural thirst.\"\n—Kerai Hatesinger, Kolaghan warrior"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c5f82b6-53d9-43ce-8a17-c680c443e586.jpg?1783938593"
        ruling(
            "2015-02-25",
            "If an effect creates a creature token that normally has toughness 2 or less, it will enter the battlefield with toughness 0 or less, be put into its owner's graveyard, and then cease to exist. Any abilities that trigger when a creature enters the battlefield or dies will trigger."
        )
    }
}
