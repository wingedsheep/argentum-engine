package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gift of Immortality
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * When enchanted creature dies, return that card to the battlefield under its owner's control.
 * Return this card to the battlefield attached to that creature at the beginning of the next end step.
 *
 * - The dead creature is gathered by its triggering reference, which names it only while it is
 *   still the same object (CR 400.7): a token (ceased to exist) or a card that left the graveyard
 *   first gathers nothing. `moveTracked` records the creature as it entered the battlefield.
 * - By resolution the Aura is in its owner's graveyard (state-based action); it is gathered from
 *   there — and only from there — and carried, with the returned creature, into a step-based
 *   end-step delayed trigger (`carryCollections`). Carried objects that have since changed zones
 *   are dropped when it fires, so a creature that left the battlefield (or blinked) before the end
 *   step leaves Gift of Immortality in the graveyard (2013-09-15 ruling), as does a Gift that left
 *   the graveyard.
 * - "The next end step" is a step-based one-shot with no turn floor: the trigger watches step
 *   *beginnings*, so a creature that dies during an end step comes back at once but the Aura waits
 *   for the following turn's end step.
 */
val GiftOfImmortality = card("Gift of Immortality") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When enchanted creature dies, return that card to the battlefield under its owner's control. " +
        "Return this card to the battlefield attached to that creature at the beginning of the next end step."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.Pipeline {
            val deadCreature = gather(CardSource.Entity(EffectTarget.TriggeringEntity), name = "giftDeadCreature")
            val returnedCreature = moveTracked(
                deadCreature,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                underOwnersControl = true,
                name = "giftReturnedCreature"
            )
            val giftCard = filter(
                gather(CardSource.Entity(EffectTarget.Self), name = "giftSelf"),
                GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD),
                name = "giftInGraveyard"
            )
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Pipeline {
                    move(
                        giftCard,
                        CardDestination.ToZone(Zone.BATTLEFIELD),
                        attachTo = returnedCreature.asTarget
                    )
                },
                carryCollections = listOf(returnedCreature.key, giftCard.key)
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2336faaa-fc6d-4fbb-bdd4-969eae565c72.jpg?1783939816"
        ruling(
            "2013-09-15",
            "If the enchanted creature is a token, neither it nor Gift of Immortality will return to the battlefield."
        )
        ruling(
            "2013-09-15",
            "If the creature is no longer on the battlefield at the beginning of the next end step after it's " +
                "returned to the battlefield, Gift of Immortality will remain in its owner's graveyard."
        )
    }
}
