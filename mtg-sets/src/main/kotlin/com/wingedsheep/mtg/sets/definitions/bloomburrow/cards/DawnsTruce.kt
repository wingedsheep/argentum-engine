package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
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
 * Gift is modeled as a modal choice. Mode 1 = no gift (hexproof),
 * Mode 2 = gift (opponent draws + hexproof + indestructible).
 */
val DawnsTruce = card("Dawn's Truce") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nYou and permanents you control gain hexproof until end of turn. If the gift was promised, permanents you control also gain indestructible until end of turn."

    val hexproofEffects = Effects.GrantHexproof(EffectTarget.Controller)
        .then(EffectPatterns.grantKeywordToAll(Keyword.HEXPROOF, Filters.Group.permanentsYouControl))

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — hexproof until end of turn
            Mode.noTarget(
                hexproofEffects,
                "Don't promise a gift — you and permanents you control gain hexproof until end of turn"
            ),
            // Mode 2: Gift a card — opponent draws, hexproof + indestructible until end of turn
            Mode.noTarget(
                DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent))
                    .then(hexproofEffects)
                    .then(EffectPatterns.grantKeywordToAll(Keyword.INDESTRUCTIBLE, Filters.Group.permanentsYouControl))
                    .then(Effects.GiftGiven()),
                "Promise a gift — an opponent draws a card, you and permanents you control gain hexproof until end of turn, permanents you control also gain indestructible until end of turn"
            )
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
