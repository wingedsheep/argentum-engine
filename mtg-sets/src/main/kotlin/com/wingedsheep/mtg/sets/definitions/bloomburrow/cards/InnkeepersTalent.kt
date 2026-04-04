package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWardToGroup
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Innkeeper's Talent {1}{G}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 *
 * At the beginning of combat on your turn, put a +1/+1 counter on target
 * creature you control.
 *
 * {G}: Level 2
 * Permanents you control with counters on them have ward {1}.
 *
 * {3}{G}: Level 3
 * If you would put one or more counters on a permanent or player, put twice
 * that many of each of those kinds of counters on that permanent or player
 * instead.
 */
val InnkeepersTalent = card("Innkeeper's Talent") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment — Class"
    oracleText = "At the beginning of combat on your turn, put a +1/+1 counter on target creature you control.\n" +
        "{G}: Level 2 — Permanents you control with counters on them have ward {1}.\n" +
        "{3}{G}: Level 3 — If you would put one or more counters on a permanent or player, put twice that many of each of those kinds of counters on that permanent or player instead."

    // Level 1: At the beginning of combat on your turn, put a +1/+1 counter on target creature you control
    triggeredAbility {
        trigger = Triggers.BeginCombat
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    // Level 2: Permanents you control with counters on them have ward {1}
    classLevel(2, "{G}") {
        staticAbility {
            ability = GrantWardToGroup(
                manaCost = "{1}",
                filter = GroupFilter(GameObjectFilter.Permanent.youControl().withAnyCounter())
            )
        }
    }

    // Level 3: Double counter placement on permanents or players
    classLevel(3, "{3}{G}") {
        replacementEffect(
            DoubleCounterPlacement(
                appliesTo = GameEvent.CounterPlacementEvent(
                    counterType = CounterTypeFilter.Any,
                    recipient = RecipientFilter.Any
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/941b0afc-0e8f-45f2-ae7f-07595e164611.jpg?1721814343"
        ruling("2024-07-26", "Once a ward ability of a permanent with a counter on it has triggered, causing that permanent to lose ward by removing Innkeeper's Talent or removing the counters from that permanent won't affect that ability.")
        ruling("2024-07-26", "If a permanent enters with counters on it, the effect causing the permanent to be given counters may specify which player puts those counters on it. If the effect doesn't specify a player, the object's controller puts those counters on it.")
        ruling("2024-07-26", "If two or more effects attempt to modify how many counters would be put onto a permanent you control, you choose the order to apply those effects, no matter who controls the sources of those effects.")
    }
}
