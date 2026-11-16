package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swift Warkite
 * {4}{B}{R}
 * Creature — Dragon
 * 4/4
 * Flying
 * When this creature enters, you may put a creature card with mana value 3 or less from your hand
 * or graveyard onto the battlefield. That creature gains haste. Return it to your hand at the
 * beginning of the next end step.
 *
 * The pick is one `chooseUpTo(1)` over hand + graveyard together (choosing nothing is the "you
 * may"). `moveTracked` records the object that actually entered, so the haste grant and the delayed
 * return follow it — and, as the rulings require, the return only moves it if it is still on the
 * battlefield when the end-step trigger resolves, and the delayed trigger fires even if Swift
 * Warkite has left the battlefield by then (same shape as Nahiri, the Harbinger's −8).
 */
val SwiftWarkite = card("Swift Warkite") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, you may put a creature card with mana value 3 or less from your " +
        "hand or graveyard onto the battlefield. That creature gains haste. Return it to your hand at " +
        "the beginning of the next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.FromMultipleZones(
                    listOf(Zone.HAND, Zone.GRAVEYARD),
                    Player.You,
                    GameObjectFilter.Creature.manaValueAtMost(3)
                )
            )
            val putting = chooseUpTo(
                1,
                from = candidates,
                prompt = "You may put a creature card with mana value 3 or less from your hand or graveyard " +
                    "onto the battlefield"
            )
            val entered = moveTracked(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(
                Effects.ForEachInCollection(
                    entered,
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent) then
                        Effects.CreateDelayedTrigger(
                            step = Step.END,
                            effect = Effects.Move(EffectTarget.IterationEntity, Zone.HAND),
                        ),
                ),
            )
        }
        description = "When Swift Warkite enters, you may put a creature card with mana value 3 or less from " +
            "your hand or graveyard onto the battlefield. That creature gains haste. Return it to your hand " +
            "at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f62db9d7-3cc7-49cb-bffc-d2f9c286aa36.jpg?1783938570"
        ruling("2015-02-25", "You can't put creatures onto the battlefield face down this way.")
        ruling(
            "2015-02-25",
            "The creature you put onto the battlefield will be returned to your hand only if it's still on the " +
                "battlefield as the delayed triggered ability resolves during the next end step. If it leaves " +
                "the battlefield before then, it will remain wherever it is."
        )
        ruling(
            "2015-02-25",
            "The delayed triggered ability that returns the creature to your hand will trigger even if Swift " +
                "Warkite leaves the battlefield before the next end step."
        )
    }
}
