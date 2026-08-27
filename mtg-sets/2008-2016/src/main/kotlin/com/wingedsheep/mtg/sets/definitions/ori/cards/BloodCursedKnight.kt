package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blood-Cursed Knight
 * {1}{W}{B}
 * Creature — Vampire Knight
 * 3/2
 * As long as you control an enchantment, this creature gets +1/+1 and has lifelink.
 */
val BloodCursedKnight = card("Blood-Cursed Knight") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "BW"
    typeLine = "Creature — Vampire Knight"
    power = 3
    toughness = 2
    oracleText = "As long as you control an enchantment, this creature gets +1/+1 and has lifelink. (Damage dealt by this creature also causes you to gain that much life.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Enchantment),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK.name, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Enchantment),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Winona Nelson"
        flavorText = "\"The bloodlust shall not control me, for my oath is my greatest compulsion.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/644bb558-113e-4e49-a395-7e0036c3419c.jpg?1783938315"

        ruling("2015-06-22", "If you cast an Aura spell targeting a creature controlled by an opponent, you still control that Aura. It will count for Blood-Cursed Knight's ability.")
    }
}
