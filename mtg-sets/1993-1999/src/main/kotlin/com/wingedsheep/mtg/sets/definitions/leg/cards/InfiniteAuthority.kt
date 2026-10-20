package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Infinite Authority
 * {W}{W}{W}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature blocks or becomes blocked by a creature with toughness 3 or less,
 * destroy the other creature at end of combat. At the beginning of the next end step, if that
 * creature was destroyed this way, put a +1/+1 counter on the first creature.
 *
 * Two delayed triggers, one created by the other. The block trigger gathers both creatures while
 * it still knows them — the partner ("it") and the enchanted creature ("the first creature",
 * read off the Aura's last host if the Aura has gone) — and each delayed trigger carries those
 * collections forward, so they keep naming the same objects (CR 603.7c). The end-of-combat one
 * destroys the partner; only a creature that was actually destroyed this way (not regenerated, not
 * indestructible) schedules the end-step counter, which settles the "if" at the moment it's decided.
 */
val InfiniteAuthority = card("Infinite Authority") {
    manaCost = "{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature blocks or becomes blocked by a creature with toughness 3 or less, " +
        "destroy the other creature at end of combat. At the beginning of the next end step, if that " +
        "creature was destroyed this way, put a +1/+1 counter on the first creature."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.blocksOrBecomesBlocked(GameObjectFilter.Creature.toughnessAtMost(3))
        effect = Effects.Pipeline {
            val other = gather(CardSource.Entity(EffectTarget.TriggeringEntity), name = "otherCreature")
            val first = gather(CardSource.Entity(EffectTarget.EnchantedCreature), name = "firstCreature")
            run(
                Effects.CreateDelayedTrigger(
                    step = Step.END_COMBAT,
                    carryCollections = listOf(other.key, first.key),
                    effect = Effects.Pipeline {
                        val destroyed = moveTracked(
                            other,
                            CardDestination.ToZone(Zone.GRAVEYARD),
                            moveType = MoveType.Destroy,
                            name = "destroyedThisWay",
                        )
                        ifNotEmpty(destroyed) {
                            run(
                                Effects.CreateDelayedTrigger(
                                    step = Step.END,
                                    carryCollections = listOf(first.key),
                                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, first.asTarget),
                                )
                            )
                        }
                    },
                )
            )
        }
        description = "Whenever enchanted creature blocks or becomes blocked by a creature with toughness " +
            "3 or less, destroy the other creature at end of combat. At the beginning of the next end step, " +
            "if that creature was destroyed this way, put a +1/+1 counter on the first creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc60077f-d577-4a6c-a78f-697317024c40.jpg?1783948085"
    }
}
