package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hidden Path
 * {2}{G}{G}{G}{G}
 * Enchantment
 * Green creatures have forestwalk.
 *
 * Every green creature on the battlefield, whoever controls it — the group is re-read from
 * projected state, so a creature that becomes green later gains forestwalk too.
 */
val HiddenPath = card("Hidden Path") {
    manaCost = "{2}{G}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Green creatures have forestwalk. (They can't be blocked as long as defending " +
        "player controls a Forest.)"

    staticAbility {
        ability = GrantKeyword(
            Keyword.FORESTWALK,
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.GREEN))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Rob Alexander"
        flavorText = "\"Where moments before we were lost beyond hope, the strange, floating lights showed us the way and restored our morale.\"\n—Vervamon the Elder"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbc93c0b-0ac8-4b8f-b2f6-96887d1acd77.jpg?1783947932"
    }
}
