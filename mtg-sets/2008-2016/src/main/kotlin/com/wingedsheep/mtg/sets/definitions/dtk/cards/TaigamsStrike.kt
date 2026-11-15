package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Taigam's Strike
 * {3}{U}
 * Sorcery
 *
 * Target creature gets +2/+0 until end of turn and can't be blocked this turn.
 * Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)
 *
 * Both halves of the first line are end-of-turn floating effects on the one target: an
 * [Effects.ModifyStats] pump then an [Effects.GrantKeyword] of [AbilityFlag.CANT_BE_BLOCKED].
 * Rebound is the bare [Keyword.REBOUND], read by `StackResolver` as the spell resolves; the
 * upkeep re-cast chooses a fresh target, and a fizzled first cast gets no rebound.
 */
val TaigamsStrike = card("Taigam's Strike") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +2/+0 until end of turn and can't be blocked this turn.\n" +
        "Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)"

    keywords(Keyword.REBOUND)

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, t) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ef5e279-14c4-4f91-b24d-c7098140efcd.jpg?1783938602"
    }
}
