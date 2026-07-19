package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hire a Crew
 * {2}{R}
 * Instant
 *
 * Create a 2/1 black Villain creature token with menace, then creatures you control get
 * +1/+0 until end of turn.
 *
 * Implementation notes:
 * - The "then" ordering matters: the token is created first, so the group pump (which snapshots
 *   the battlefield when it starts iterating) also buffs the fresh Villain, making it a 3/1.
 */
val HireACrew = card("Hire a Crew") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Create a 2/1 black Villain creature token with menace, then creatures you " +
        "control get +1/+0 until end of turn. (A creature with menace can't be blocked except " +
        "by two or more creatures.)"

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf(Subtype.VILLAIN.value),
            keywords = setOf(Keyword.MENACE),
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a51b6a0-9a54-4f01-b959-0a28c15d103f.jpg?1783902804"
        ).then(
            Patterns.Group.modifyStatsForAll(
                power = 1,
                toughness = 0,
                filter = GroupFilter.AllCreaturesYouControl,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Lordigan"
        flavorText = "\"You want subtle, you call the other guys. We're the Wrecking Crew.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50a6aa80-79a3-46f3-9cc1-5bf663b64976.jpg?1783902933"
    }
}
