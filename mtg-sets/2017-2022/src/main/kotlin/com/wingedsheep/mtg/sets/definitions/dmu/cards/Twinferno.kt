package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Twinferno
 * {1}{R}
 * Instant
 * Choose one —
 * • When you cast your next instant or sorcery spell this turn, copy that spell. You may choose new targets for the copy.
 * • Target creature you control gains double strike until end of turn.
 */
val Twinferno = card("Twinferno") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n• When you cast your next instant or sorcery spell this turn, copy that spell. You may choose new targets for the copy.\n• Target creature you control gains double strike until end of turn. (It deals both first-strike and regular combat damage.)"

    spell {
        modal(chooseCount = 1) {
            mode("Copy your next instant or sorcery spell this turn") {
                effect = Effects.CopyNextSpellCast(1)
            }
            mode("Target creature you control gains double strike until end of turn") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Justyna Dura"
        flavorText = "\"More fire?\" Jaya asked.\n\"More fire,\" Jodah agreed."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc531409-26cb-4c58-ba69-f39dd8aee3d1.jpg?1783921308"
        ruling("2022-09-09", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy created by Twinferno's delayed triggered ability will have the same mode or modes. You can't choose different ones.")
        ruling("2022-09-09", "The copies that Twinferno's ability creates are created on the stack, so they're not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
    }
}
