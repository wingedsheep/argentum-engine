package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Boiling Earth
 * {1}{R}
 * Sorcery
 * Boiling Earth deals 1 damage to each creature your opponents control.
 * Awaken 4—{6}{R} (If you cast this spell for {6}{R}, also put four +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * Cast for its awaken cost, the land is the spell's only target: if it's gone by resolution the
 * whole spell doesn't resolve, so no damage is dealt.
 */
val BoilingEarth = card("Boiling Earth") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Boiling Earth deals 1 damage to each creature your opponents control.\n" +
        "Awaken 4—{6}{R} (If you cast this spell for {6}{R}, also put four +1/+1 counters on target " +
        "land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(4, "{6}{R}"))

    spell {
        effect = Patterns.Group.dealDamageToAll(1, GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdaab44c-4ce1-43fb-915c-c687fe8559ce.jpg?1783938195"
    }
}
