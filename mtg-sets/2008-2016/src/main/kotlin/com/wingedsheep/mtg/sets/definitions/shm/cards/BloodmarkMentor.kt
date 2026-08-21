package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bloodmark Mentor
 * {1}{R}
 * Creature — Goblin Warrior
 * 1 / 1
 *
 * Red creatures you control have first strike.
 *
 * - No "Other": the Mentor is red itself, so the [GroupFilter] deliberately has no `excludeSelf`
 *   and it grants first strike to itself as well.
 * - The colour test goes through the group filter so it reads projected state — a creature that is
 *   only red because of a continuous effect gains first strike too, and loses it again when the
 *   effect ends.
 */
val BloodmarkMentor = card("Bloodmark Mentor") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Red creatures you control have first strike."

    staticAbility {
        ability = GrantKeyword(
            Keyword.FIRST_STRIKE,
            GroupFilter(GameObjectFilter.Creature.withColor(Color.RED).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Dave Allsop"
        flavorText = "Boggarts divide the world into two categories: things you can eat, and things you have to chase down and pummel before you can eat."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6322a389-44cc-4a3d-bd39-93c156640f8e.jpg?1783942750"
    }
}
