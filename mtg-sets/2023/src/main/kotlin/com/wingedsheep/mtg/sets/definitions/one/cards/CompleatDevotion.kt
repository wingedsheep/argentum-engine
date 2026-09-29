package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Compleat Devotion
 * {1}{W}
 * Instant
 * Target creature you control gets +2/+2 until end of turn. If that creature has toxic, draw a card.
 *
 * "Has toxic" is `withKeyword(TOXIC)`, which matches any toxic N — printed or granted — off the
 * projected `TOXIC_<n>` keyword.
 */
val CompleatDevotion = card("Compleat Devotion") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +2/+2 until end of turn. If that creature has toxic, draw a card."

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(2, 2, t) then Effects.If(
            condition = Conditions.TargetMatchesFilter(Filters.Creature.withKeyword(Keyword.TOXIC), t),
            then = Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Filipe Pagliuso"
        flavorText = "\"Elspeth, my friend, the Multiverse has answered my prayers to bring you here so that I may gather you into my pride and forever protect you.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bb29a69-3e42-4268-8026-f01072d7b56c.jpg?1783918085"
    }
}
