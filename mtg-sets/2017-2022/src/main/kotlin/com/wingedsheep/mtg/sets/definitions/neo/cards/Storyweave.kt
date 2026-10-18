package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storyweave — Kamigawa: Neon Dynasty #209 (canonical printing)
 * {2}{G} · Instant
 *
 * Choose one —
 * • Put two +1/+1 counters on target creature you control.
 * • Put two lore counters on target Saga you control. The next time one or more enchantment
 *   creatures you control enter this turn, each enters with two additional +1/+1 counters on it.
 *
 * The second mode's rider is a one-shot replacement the resolving spell grants its caster:
 * `GrantReplacementEffect(EntersWithCounters(…), Self, Duration.NextUse)`. It modifies only the
 * first entry event that brings in a matching creature — every enchantment creature entering in
 * that event gets the counters — and is then spent; unused, it lapses at end of turn. That is what
 * makes the classic play work: the two lore counters can reach a NEO Saga's last chapter, which
 * exiles it and returns it transformed as an enchantment creature with the counters.
 */
val Storyweave = card("Storyweave") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Put two +1/+1 counters on target creature you control.\n" +
        "• Put two lore counters on target Saga you control. The next time one or more enchantment " +
        "creatures you control enter this turn, each enters with two additional +1/+1 counters on it."

    spell {
        modal(chooseCount = 1) {
            mode("Put two +1/+1 counters on target creature you control") {
                val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
            }
            mode("Put two lore counters on target Saga you control; the next enchantment creatures you control to enter this turn get two +1/+1 counters") {
                val saga = target(TargetFilter(GameObjectFilter.Enchantment.withSubtype(Subtype.SAGA).youControl()))
                effect = Effects.AddCounters(CounterType.LORE, 2, saga) then Effects.GrantReplacementEffect(
                    EntersWithCounters(
                        counterType = CounterType.PLUS_ONE_PLUS_ONE,
                        count = 2,
                        appliesTo = EventPattern.ZoneChangeEvent(
                            filter = GameObjectFilter.Creature.youControl() and GameObjectFilter.Enchantment,
                            to = Zone.BATTLEFIELD
                        )
                    ),
                    EffectTarget.Self,
                    Duration.NextUse("one or more enchantment creatures you control enter")
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50a77eed-aa03-43b7-b3c8-6aee4c96c648.jpg?1783923840"
    }
}
