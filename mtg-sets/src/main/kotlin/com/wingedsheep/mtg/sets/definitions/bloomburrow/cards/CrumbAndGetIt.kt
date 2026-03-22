package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crumb and Get It
 * {W}
 * Instant
 *
 * Gift a Food (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a Food token before its other effects.
 * It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 *
 * Target creature you control gets +2/+2 until end of turn.
 * If the gift was promised, that creature also gains indestructible until end of turn.
 */
val CrumbAndGetIt = card("Crumb and Get It") {
    manaCost = "{W}"
    typeLine = "Instant"
    oracleText = "Gift a Food (You may promise an opponent a gift as you cast this spell. If you do, they create a Food token before its other effects. It's an artifact with \"{2}, {T}, Sacrifice this artifact: You gain 3 life.\")\nTarget creature you control gets +2/+2 until end of turn. If the gift was promised, that creature also gains indestructible until end of turn."

    val baseEffect = Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0))

    spell {
        val creature = target("creature", Targets.CreatureYouControl)
        effect = OptionalCostEffect(
            cost = Effects.CreateFood(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            ifPaid = baseEffect
                .then(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)),
            ifNotPaid = baseEffect
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c7b3b25-d4b3-4451-9f5c-6eb369541175.jpg?1721429540"

        ruling("2024-07-26", "As an additional cost to cast a spell with gift, you can promise the listed gift to an opponent. That opponent is chosen as part of that additional cost.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given. None of its other effects will happen either.")
    }
}
