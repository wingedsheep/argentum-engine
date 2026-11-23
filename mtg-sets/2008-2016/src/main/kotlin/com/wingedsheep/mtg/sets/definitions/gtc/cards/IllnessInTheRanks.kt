package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Illness in the Ranks
 * {B}
 * Enchantment
 * Creature tokens get -1/-1.
 *
 * Applies to every creature token on the battlefield, under any player's control (Virulent
 * Plague's shape at -1/-1).
 */
val IllnessInTheRanks = card("Illness in the Ranks") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Creature tokens get -1/-1."

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = -1,
            filter = GroupFilter(GameObjectFilter.Creature.token())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Nils Hamm"
        flavorText = "\"Selesnya denied us tribute. Let's show them the error of their ways.\"\n—Milana, Orzhov prelate"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/989a68c1-3b76-4c2d-9db3-23c45be3f9ff.jpg?1783940130"
        ruling(
            "2013-01-24",
            "If an effect creates a creature token that normally has toughness 1, it will enter with toughness 0, be put into its owner's graveyard as a state-based action, and then cease to exist. Any abilities that trigger when a creature enters or dies will trigger."
        )
    }
}
