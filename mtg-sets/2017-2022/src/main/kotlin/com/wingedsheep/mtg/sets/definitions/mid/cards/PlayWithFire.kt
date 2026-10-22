package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.SourceDealtDamageToPlayerThisTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Play with Fire
 * {R}
 * Instant
 * Play with Fire deals 2 damage to any target. If a player is dealt damage this way, scry 1.
 *
 * "If a player is dealt damage this way" reads what actually happened, not what was targeted:
 * the spell's own per-recipient damage memory (stamped by every damage path, after prevention and
 * redirection) is asked, for each player, whether this spell dealt them damage. The spell is a
 * fresh object on the stack, so "this turn" on that memory is exactly "this way". A creature or
 * planeswalker target, or a player whose damage was fully prevented, gets no scry.
 */
val PlayWithFire = card("Play with Fire") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Play with Fire deals 2 damage to any target. If a player is dealt damage this way, " +
        "scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(2, anyTarget) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.countPlayersWith(Player.Each, SourceDealtDamageToPlayerThisTurn(Player.You)),
                    ComparisonOperator.GTE,
                    1
                ),
                then = Patterns.Library.scry(1)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Svetlin Velinov"
        flavorText = "Devils find human dwellings to be oppressively cold, dark, and unburnt."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f1a6c60-a8c4-44c2-b1ea-d3befbabdf43.jpg?1783925592"
    }
}
