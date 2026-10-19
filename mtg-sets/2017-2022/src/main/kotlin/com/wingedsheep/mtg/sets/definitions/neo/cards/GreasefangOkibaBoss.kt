package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Greasefang, Okiba Boss — Kamigawa: Neon Dynasty #220 (canonical printing)
 * {1}{W}{B} · Legendary Creature — Rat Pilot · 4/3
 *
 * At the beginning of combat on your turn, return target Vehicle card from your graveyard to the
 * battlefield. It gains haste. Return it to its owner's hand at the beginning of your next end step.
 *
 * Cauldron Dance's reanimate-haste-bounce, with two refinements. The bounce is a step-based delayed
 * trigger gated to *your* end step (`fireOnPlayer` + [DelayedTriggerTiming.NEXT_END_STEP]), and the
 * returned Vehicle travels to it as a carried collection, which captures the object's identity:
 * if the Vehicle leaves the battlefield before then (it's destroyed, or bounced and recast), it is
 * a new object and the delayed trigger no longer touches it (CR 603.7c).
 */
val GreasefangOkibaBoss = card("Greasefang, Okiba Boss") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Rat Pilot"
    power = 4
    toughness = 3
    oracleText = "At the beginning of combat on your turn, return target Vehicle card from your graveyard " +
        "to the battlefield. It gains haste. Return it to its owner's hand at the beginning of your next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        target(TargetFilter(GameObjectFilter.Any.withSubtype(Subtype.VEHICLE).ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val returned = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                name = "greasefangReturned",
            )
            ifNotEmpty(returned) {
                run(Effects.GrantKeyword(Keyword.HASTE, returned.asTarget, Duration.Permanent))
                run(Effects.CreateDelayedTrigger(
                    step = Step.END,
                    fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                    timing = DelayedTriggerTiming.NEXT_END_STEP,
                    effect = Effects.Pipeline {
                        move(returned, CardDestination.ToZone(Zone.HAND))
                    },
                    carryCollections = listOf(returned.key),
                ))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Victor Adame Minguez"
        flavorText = "It's hard to say which is more deadly: her bike or her bite."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56a0d43b-4d38-40a7-be6c-8324ab3bf773.jpg?1783923836"
        ruling("2022-02-18", "A Vehicle with haste must still be crewed (or become a creature some other way) before it can attack.")
    }
}
