package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Elspeth Conquers Death
 * {3}{W}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Exile target permanent an opponent controls with mana value 3 or greater.
 * II — Noncreature spells your opponents cast cost {2} more to cast until your next turn.
 * III — Return target creature or planeswalker card from your graveyard to the battlefield.
 * Put a +1/+1 counter or a loyalty counter on it.
 *
 * Chapter II is a resolved tax on the game state (`IncreaseSpellCosts` with
 * `UntilYourNextTurn`), so it outlives the Saga and ends when *your* next turn begins.
 * Chapter III's counter is chosen after the card has entered (per the ruling, either kind
 * may go on either permanent type).
 */
val ElspethConquersDeath = card("Elspeth Conquers Death") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Exile target permanent an opponent controls with mana value 3 or greater.\n" +
        "II — Noncreature spells your opponents cast cost {2} more to cast until your next turn.\n" +
        "III — Return target creature or planeswalker card from your graveyard to the battlefield. " +
        "Put a +1/+1 counter or a loyalty counter on it."

    sagaChapter(1) {
        val permanent = target(TargetFilter(GameObjectFilter.Permanent.opponentControls().manaValueAtLeast(3)))
        effect = Effects.Exile(permanent)
    }

    sagaChapter(2) {
        effect = Effects.IncreaseSpellCosts(
            DynamicAmounts.fixed(2),
            player = Player.EachOpponent,
            spellFilter = GameObjectFilter.Noncreature,
            duration = Duration.UntilYourNextTurn,
        )
    }

    sagaChapter(3) {
        val card = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        label = "Put a +1/+1 counter on it",
                        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, card),
                    ),
                    EffectChoice(
                        label = "Put a loyalty counter on it",
                        effect = Effects.AddCounters(CounterType.LOYALTY, 1, card),
                    ),
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea20208b-1939-4c69-8cfd-c0a42f9dc427.jpg?1783931598"
        ruling(
            "2020-01-24",
            "The effect of the second chapter lasts until your next turn even if this Saga leaves the " +
                "battlefield before then.",
        )
        ruling(
            "2020-01-24",
            "You choose which kind of counter to put on the returned permanent after it has entered the " +
                "battlefield. You may put a +1/+1 counter on a planeswalker or a loyalty counter on a " +
                "creature, but this isn't normally going to have any effect.",
        )
    }
}
