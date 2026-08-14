package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Robot Domination — Marvel Super Heroes #111
 * {3}{B} · Enchantment — Plan
 *
 * Whenever one or more creature cards are put into your graveyard from anywhere, you draw a card,
 * lose 1 life, and put a plan counter on this enchantment.
 * When the third plan counter is put on this enchantment, sacrifice it and create three 2/2
 * colorless Robot Villain artifact creature tokens.
 *
 * Modeling notes:
 *  - The accumulator is the batching [Triggers.CardsPutIntoYourGraveyard]`(Creature)` — one fire
 *    per event batch no matter how many creature cards land, and regardless of the source zone.
 *  - "When the **third** plan counter is put on this enchantment" composes from existing
 *    vocabulary: a SELF-bound [Triggers.countersPlacedOn] on [Counters.PLAN] gated by
 *    `triggerCondition = `[Conditions.SourceCounterCountAtLeast]`(PLAN, 3)`. The at-least gate is
 *    behaviourally exact here because the payoff **sacrifices its own source**, so the enchantment
 *    is gone before a fourth counter could ever land — the threshold can never fire twice. No
 *    dedicated "Nth counter" trigger event is needed.
 *  - The payoff has no "when you do" clause, so it is a plain sequential composite rather than a
 *    reflexive trigger.
 */
val RobotDomination = card("Robot Domination") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Plan"
    oracleText = "Whenever one or more creature cards are put into your graveyard from anywhere, " +
        "you draw a card, lose 1 life, and put a plan counter on this enchantment.\n" +
        "When the third plan counter is put on this enchantment, sacrifice it and create three " +
        "2/2 colorless Robot Villain artifact creature tokens."

    triggeredAbility {
        trigger = Triggers.CardsPutIntoYourGraveyard(GameObjectFilter.Creature)
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.LoseLife(1, EffectTarget.Controller),
            Effects.AddCounters(Counters.PLAN, 1, EffectTarget.Self),
        )
        description = "Whenever one or more creature cards are put into your graveyard from " +
            "anywhere, you draw a card, lose 1 life, and put a plan counter on this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Any,
            counterType = Counters.PLAN,
            firstTimeEachTurn = false,
            binding = TriggerBinding.SELF,
        )
        triggerCondition = Conditions.SourceCounterCountAtLeast(Counters.PLAN, 3)
        effect = Effects.Composite(
            Effects.SacrificeTarget(EffectTarget.Self),
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = emptySet(),
                creatureTypes = setOf(Subtype.ROBOT.value, Subtype.VILLAIN.value),
                count = 3,
                artifactToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/8/e/8eb1de03-fc45-45bd-bd1f-5b164104426e.jpg?1783902799",
            ),
        )
        description = "When the third plan counter is put on this enchantment, sacrifice it and " +
            "create three 2/2 colorless Robot Villain artifact creature tokens."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b26bb968-6612-43fe-9147-a3d4786cbc20.jpg?1783902938"
    }
}
