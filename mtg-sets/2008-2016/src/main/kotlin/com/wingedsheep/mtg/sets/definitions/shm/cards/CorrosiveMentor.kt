package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Corrosive Mentor
 * {2}{B}
 * Creature — Elemental Rogue
 * 1 / 3
 *
 * Black creatures you control have wither. (They deal damage to creatures in the form of -1/-1 counters.)
 *
 * - No "Other": the Mentor is black itself, so the [GroupFilter] deliberately has no `excludeSelf`
 *   and it grants wither to itself as well.
 * - Wither is a granted keyword here, not a printed one, so it goes through [GrantKeyword] rather
 *   than `keywords(...)` — the runtime keyword resolver is what the combat damage step reads.
 * - The colour test goes through the group filter so it reads projected state — a creature that is
 *   only black because of a continuous effect gains wither too.
 */
val CorrosiveMentor = card("Corrosive Mentor") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Elemental Rogue"
    power = 1
    toughness = 3
    oracleText = "Black creatures you control have wither. (They deal damage to creatures in the form of -1/-1 counters.)"

    staticAbility {
        ability = GrantKeyword(
            Keyword.WITHER,
            GroupFilter(GameObjectFilter.Creature.withColor(Color.BLACK).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Daarken"
        flavorText = "Guttering cinders stoke their dying flames by snuffing out the lights of others."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/140457ff-ee7d-48ec-8b91-ef5c2cc1ed74.jpg?1783942756"
    }
}
