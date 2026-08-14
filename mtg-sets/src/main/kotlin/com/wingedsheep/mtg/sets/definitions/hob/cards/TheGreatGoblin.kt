package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * The Great Goblin — The Hobbit #158
 * {1}{B/R}{B/R} · Legendary Creature — Goblin Noble · Rare
 * 3/2
 *
 * Whenever you put one or more counters on a Goblin, Orc, or Army you control, The Great Goblin
 * deals 2 damage to target opponent.
 * Whenever another Goblin, Orc, or Army you control dies, exile the top card of your library. You
 * may play it until the end of your next turn.
 *
 * Modeling notes:
 *  - The counters trigger is [Triggers.countersPlacedOn] with `firstTimeEachTurn = false`: the
 *    printed wording has no once-per-turn gate, so every batch of counters fires it again. It is
 *    "one or more counters", so a single placement of three counters still fires exactly once — the
 *    engine's `CountersPlacedEvent` is already per-placement, not per-counter.
 *  - `placedBy = Player.You` carries the "**you** put" scope (CR 122.6a). It can't come from the
 *    recipient filter: an opponent's effect putting counters on a Goblin *you control* matches the
 *    filter but is not your placement, and must not fire. Amass is the common in-set source, and
 *    since amass is worded as "that player puts", an opponent amassing their own Army correctly
 *    leaves The Great Goblin cold even though [AzogMoriasRuin] made them do it.
 *  - The Great Goblin is itself a Goblin and the first ability has no "another", so counters landing
 *    on it (a +1/+1 counter from an Equipment, say) fire it — hence [TriggerBinding.ANY] and a
 *    filter with no self-exclusion.
 *  - The death trigger *is* "another", so it takes [TriggerBinding.OTHER]; it is per-creature
 *    (not the batched `OneOrMore…Die` shape), matching the printed singular wording — a board wipe
 *    that kills three Goblins exiles three cards.
 *  - Both abilities use one OR over subtypes ([GameObjectFilter.withAnySubtype]) rather than three
 *    filters; an Orc Army satisfies it once, not twice. See [GoblinTown] for the same idiom.
 *  - The impulse window is [MayPlayExpiry.UntilEndOfNextTurn], turn-keyed, so it survives both the
 *    end of the current turn and The Great Goblin leaving the battlefield.
 */
val TheGreatGoblin = card("The Great Goblin") {
    manaCost = "{1}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Goblin Noble"
    power = 3
    toughness = 2
    oracleText = "Whenever you put one or more counters on a Goblin, Orc, or Army you control, " +
        "The Great Goblin deals 2 damage to target opponent.\n" +
        "Whenever another Goblin, Orc, or Army you control dies, exile the top card of your " +
        "library. You may play it until the end of your next turn."

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Creature.youControl()
                .withAnySubtype("Goblin", "Orc", "Army"),
            counterType = Counters.ANY,
            firstTimeEachTurn = false,
            binding = TriggerBinding.ANY,
            placedBy = Player.You,
        )
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.DealDamage(2, opponent)
        description = "The Great Goblin deals 2 damage to target opponent."
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl()
                .withAnySubtype("Goblin", "Orc", "Army"),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER,
        )
        effect = Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.UntilEndOfNextTurn)
        description = "Exile the top card of your library. You may play it until the end of your " +
            "next turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Miklós Ligeti"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78d8f53e-537d-4eaa-99e3-cac57fa53d22.jpg?1784798171"
    }
}
