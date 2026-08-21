package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Roughshod Mentor
 * {5}{G}
 * Creature — Giant Warrior
 * 5 / 4
 *
 * Green creatures you control have trample.
 *
 * - No "Other": the Mentor is green itself, so the [GroupFilter] deliberately has no `excludeSelf`
 *   and it grants trample to itself as well.
 * - Trample is granted, not printed, so it goes through [GrantKeyword] rather than `keywords(...)`.
 * - The colour test goes through the group filter so it reads projected state — a creature that is
 *   only green because of a continuous effect gains trample too.
 */
val RoughshodMentor = card("Roughshod Mentor") {
    manaCost = "{5}{G}"
    typeLine = "Creature — Giant Warrior"
    power = 5
    toughness = 4
    oracleText = "Green creatures you control have trample."

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.withColor(Color.GREEN).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Steven Belledin"
        flavorText = "He didn't hear the cries of the treefolk whose branches he snapped or of the elves caught underfoot. He had eyes only for the path ahead."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abe77862-516f-4eb2-9a41-0c6401d02843.jpg?1783942740"
    }
}
