package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Descent into Madness
 * {3}{B}{B}
 * Enchantment
 *
 * At the beginning of your upkeep, put a despair counter on this enchantment, then each player
 * exiles X permanents they control and/or cards from their hand, where X is the number of despair
 * counters on this enchantment.
 *
 * Rulings (2012-05-01): the active player chooses first, then each other player in turn order, and
 * every chosen card is exiled at the same time — `forEachPlayerCollecting(ActivePlayerFirst)` gathers
 * the picks and one `exile` moves them. A player with fewer than X cards in hand and permanents
 * exiles all of them. If the enchantment has left the battlefield by resolution, X is the number of
 * despair counters it had when it left — `countersOnSelf` falls back to its last-known count.
 */
val DescentIntoMadness = card("Descent into Madness") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, put a despair counter on this enchantment, then each " +
        "player exiles X permanents they control and/or cards from their hand, where X is the number of " +
        "despair counters on this enchantment."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        description = "Put a despair counter on this enchantment, then each player exiles X permanents they " +
            "control and/or cards from their hand, where X is the number of despair counters on this enchantment."
        effect = Effects.AddCounters(CounterType.DESPAIR, 1, EffectTarget.Self) then Effects.Pipeline {
            val (chosen) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val pool = gather(CardSource.FromMultipleZones(listOf(Zone.HAND, Zone.BATTLEFIELD), Player.You))
                listOf(
                    chooseExactly(
                        DynamicAmounts.countersOnSelf(CounterType.DESPAIR),
                        from = pool,
                        chooser = Chooser.Controller,
                        prompt = "Exile permanents you control and/or cards from your hand"
                    )
                )
            }
            exile(chosen)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "97"
        artist = "Anthony Francisco"
        flavorText = "The stairs lead down in both directions."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa016bb7-ad8b-40d5-90db-412a9cf19e4e.jpg?1783940701"
    }
}
