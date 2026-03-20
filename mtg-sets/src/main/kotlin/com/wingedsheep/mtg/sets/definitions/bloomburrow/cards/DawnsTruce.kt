package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dawn's Truce
 * {1}{W}
 * Instant
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 *
 * You and permanents you control gain hexproof until end of turn.
 * If the gift was promised, permanents you control also gain indestructible
 * until end of turn.
 *
 * The Gift mechanic is modeled using OptionalCostEffect where the "cost" is
 * the opponent drawing a card. If the player chooses to gift, the opponent draws
 * and then all effects (hexproof + indestructible) apply. If not, only hexproof applies.
 */
val DawnsTruce = card("Dawn's Truce") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nYou and permanents you control gain hexproof until end of turn. If the gift was promised, permanents you control also gain indestructible until end of turn."

    val hexproofEffects = Effects.GrantHexproof(EffectTarget.Controller)
        .then(EffectPatterns.grantKeywordToAll(Keyword.HEXPROOF, Filters.Group.permanentsYouControl))

    spell {
        effect = OptionalCostEffect(
            cost = DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            ifPaid = hexproofEffects
                .then(EffectPatterns.grantKeywordToAll(Keyword.INDESTRUCTIBLE, Filters.Group.permanentsYouControl)),
            ifNotPaid = hexproofEffects
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f72bfa0-efef-48ce-aff8-d5818ed71ba6.jpg?1721425804"

        ruling("2024-07-26", "As an additional cost to cast a spell with gift, you can promise the listed gift to an opponent. That opponent is chosen as part of that additional cost.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given. None of its other effects will happen either.")
    }
}
