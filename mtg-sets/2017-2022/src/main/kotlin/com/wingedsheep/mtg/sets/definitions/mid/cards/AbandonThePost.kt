package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Abandon the Post
 * {1}{R}
 * Sorcery
 * Up to two target creatures can't block this turn.
 * Flashback {3}{R}
 *
 * Seismic Shift's "up to two target creatures can't block" half: one optional two-count target
 * declaration, with the can't-block applied per chosen target.
 */
val AbandonThePost = card("Abandon the Post") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Up to two target creatures can't block this turn.\n" +
        "Flashback {3}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.CantBlock(EffectTarget.ContextTarget(0)))
    }

    keywordAbility(KeywordAbility.flashback("{3}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Zoltan Boros"
        flavorText = "You don't have to outrun the werewolf; you only have to outrun your fellow guard."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/983da138-f0f5-46c4-90c2-d3c34cc37d1f.jpg"
    }
}
