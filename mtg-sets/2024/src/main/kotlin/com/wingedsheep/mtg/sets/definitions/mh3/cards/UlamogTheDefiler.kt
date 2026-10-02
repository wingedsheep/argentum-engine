package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ulamog, the Defiler
 * {10}
 * Legendary Creature — Eldrazi
 * 7/7
 * When you cast this spell, target opponent exiles the top half of their library, rounded up.
 * Ward—Sacrifice two permanents.
 * Ulamog enters with a number of +1/+1 counters on it equal to the greatest mana value among cards in exile.
 * Ulamog has annihilator X, where X is the number of +1/+1 counters on it.
 *
 * The entry count is measured before Ulamog leaves its previous zone (the enters-with replacement
 * modifies the entry event itself), so an Ulamog put onto the battlefield from exile sees itself among
 * the cards in exile — per the 2024 ruling, usually ten counters. Cast from hand it is on the stack,
 * not in exile, and counts only what its cast trigger (or anything else) put there.
 *
 * Annihilator X is lowered, like every annihilator in the corpus, as the attack trigger the keyword
 * abbreviates; X is read from the counters on Ulamog when that trigger resolves.
 */
val UlamogTheDefiler = card("Ulamog, the Defiler") {
    manaCost = "{10}"
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi"
    power = 7
    toughness = 7
    oracleText = "When you cast this spell, target opponent exiles the top half of their library, rounded up.\n" +
        "Ward—Sacrifice two permanents.\n" +
        "Ulamog enters with a number of +1/+1 counters on it equal to the greatest mana value among cards in exile.\n" +
        "Ulamog has annihilator X, where X is the number of +1/+1 counters on it."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Library.exileTop(
            count = DynamicAmounts.zone(opponent.asPlayer, Zone.LIBRARY).count() divRoundedUp 2,
            target = opponent
        )
        description = "When you cast this spell, target opponent exiles the top half of their library, rounded up."
    }

    keywordAbility(KeywordAbility.Ward(WardCost.Sacrifice(GameObjectFilter.Permanent, count = 2)))

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.zone(Player.Each, Zone.EXILE).maxManaValue()
        )
    )

    // Annihilator X — the attack trigger the keyword abbreviates.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Sacrifice(
            GameObjectFilter.Permanent,
            DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
            EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
        description = "Annihilator X, where X is the number of +1/+1 counters on Ulamog."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "15"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd00d56a-86bd-41d8-82b6-975404ef8067.jpg?1783911305"

        ruling(
            "2024-06-07",
            "Ulamog, the Defiler's first ability will resolve before Ulamog does. If Ulamog is " +
                "countered or otherwise leaves the stack in response to its triggered ability, the " +
                "triggered ability will still resolve as normal."
        )
        ruling(
            "2024-06-07",
            "If Ulamog is entering the battlefield directly from exile, it will see itself when " +
                "determining which card has the greatest mana value among cards in exile. If that's " +
                "Ulamog, which seems likely, it will enter with ten +1/+1 counters on it."
        )
        ruling(
            "2024-06-07",
            "Use the number of +1/+1 counters on Ulamog at the time its annihilator ability resolves " +
                "to determine how many permanents defending player should sacrifice."
        )
    }
}
