package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Etrata, the Silencer — Guilds of Ravnica #170
 * {2}{U}{B} · Legendary Creature — Vampire Assassin · 3/5
 *
 * Etrata can't be blocked.
 * Whenever Etrata deals combat damage to a player, exile target creature that player controls and
 * put a hit counter on that card. That player loses the game if they own three or more exiled cards
 * with hit counters on them. Etrata's owner shuffles Etrata into their library.
 *
 * Pure composition:
 * - "that player" is the damaged player, bound as `Player.TriggeringPlayer` (the Alela, Cunning
 *   Conqueror targeting shape).
 * - `Effects.Exile(…, addCounterType = HIT)` puts the hit counter on the card as it lands in exile.
 * - The loss check reads that player's exile zone (exile is keyed by owner, so "they own") for
 *   nontoken cards with a hit counter — an exiled token never counts (2018-10-05 ruling).
 * - Etrata moves to its owner's library only if it is still on the battlefield, but the owner
 *   shuffles either way (2018-10-05 ruling: "it remains in its new zone and Etrata's owner shuffles
 *   their library").
 */
val EtrataTheSilencer = card("Etrata, the Silencer") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Vampire Assassin"
    power = 3
    toughness = 5
    oracleText = "Etrata can't be blocked.\n" +
        "Whenever Etrata deals combat damage to a player, exile target creature that player controls and " +
        "put a hit counter on that card. That player loses the game if they own three or more exiled cards " +
        "with hit counters on them. Etrata's owner shuffles Etrata into their library."

    flags(AbilityFlag.CANT_BE_BLOCKED)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer())
        )
        val damagedPlayer = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        effect = Effects.Exile(creature, addCounterType = CounterType.HIT) then
            Effects.If(
                Conditions.CompareAmounts(
                    DynamicAmounts.zone(
                        Player.TriggeringPlayer,
                        Zone.EXILE,
                        GameObjectFilter.Any.nontoken().withCounter(CounterType.HIT),
                    ).count(),
                    ComparisonOperator.GTE,
                    3,
                ),
                then = Effects.LoseGame(damagedPlayer),
            ) then
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.BATTLEFIELD) then
            Effects.ShuffleLibrary(EffectTarget.PlayerRef(Player.OwnerOfSource))
        description = "Whenever Etrata deals combat damage to a player, exile target creature that player " +
            "controls and put a hit counter on that card. That player loses the game if they own three or " +
            "more exiled cards with hit counters on them. Etrata's owner shuffles Etrata into their library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa36b142-e67e-49da-9080-c5994e275266.jpg?1783934135"
        ruling(
            "2018-10-05",
            "If the player controls no creatures, Etrata's ability doesn't resolve. Etrata remains on the " +
                "battlefield and its owner doesn't shuffle their library. The same is true if the target " +
                "creature is an illegal target when Etrata's ability tries to resolve."
        )
        ruling(
            "2018-10-05",
            "If Etrata leaves the battlefield while its triggered ability is on the stack, it remains in its " +
                "new zone and Etrata's owner shuffles their library."
        )
        ruling(
            "2018-10-05",
            "If Etrata's ability exiles a token creature, it won't count to determine whether three cards " +
                "are exiled with hit counters."
        )
        ruling(
            "2018-10-05",
            "Etrata's owner shuffles it into their library as the triggered ability resolves, regardless of " +
                "how many cards are in exile or whether the player loses the game."
        )
    }
}
