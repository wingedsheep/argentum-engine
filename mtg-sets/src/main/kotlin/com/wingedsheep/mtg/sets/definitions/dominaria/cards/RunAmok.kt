package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Run Amok
 * {1}{R}
 * Instant
 * Target attacking creature gets +3/+3 and gains trample until end of turn.
 */
val RunAmok = card("Run Amok") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Target attacking creature gets +3/+3 and gains trample until end of turn."

    spell {
        val t = target("target", Targets.AttackingCreature)
        effect = Effects.ModifyStats(3, 3, t)
            .then(Effects.GrantKeyword(Keyword.TRAMPLE, t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Svetlin Velinov"
        flavorText = "Keld faced an apocalypse to fulfill a dire prophecy. Now Keldons have nothing left to believe in except themselves."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3f2aa2c-deec-4927-a2d6-f5dce5967e26.jpg?1562743562"
    }
}
