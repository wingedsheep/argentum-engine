package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Clone Saga (Marvel's Spider-Man, #28)
 * {3}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Surveil 3.
 * II — When you next cast a creature spell this turn, copy it, except the copy isn't legendary.
 * III — Choose a card name. Whenever a creature with the chosen name deals combat damage to a
 * player this turn, draw a card.
 *
 * Implementation:
 *  - **I** — `Effects.Surveil(3)`.
 *  - **II** — a one-shot event-based delayed trigger on `Triggers.YouCastCreature` (SpellCastEvent,
 *    Player.You, creature) whose effect copies the triggering spell with `removeLegendary = true`
 *    (the copy is a non-legendary token — same primitive as Jackal, Genius Geneticist). `fireOnce`
 *    makes it "the next" creature spell; `TriggeringEntity` binds to the just-cast spell at fire
 *    time and the trigger resolves above it while it's still on the stack.
 *  - **III** — `Effects.ChooseCardName` stores the name in `chosenValues["clonedName"]`, then a
 *    delayed combat-damage trigger keyed to a creature *named that* (`namedFromVariable`) draws a
 *    card each time such a creature deals combat damage to a player this turn. The chosen name is
 *    baked into the trigger's source filter at creation time (`CreateDelayedTriggerExecutor
 *    .bakeChosenValuesIntoTrigger`).
 */
val TheCloneSaga = card("The Clone Saga") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice " +
        "after III.)\n" +
        "I — Surveil 3.\n" +
        "II — When you next cast a creature spell this turn, copy it, except the copy isn't " +
        "legendary.\n" +
        "III — Choose a card name. Whenever a creature with the chosen name deals combat damage " +
        "to a player this turn, draw a card."

    // I — Surveil 3.
    sagaChapter(1) {
        effect = Effects.Surveil(3)
    }

    // II — When you next cast a creature spell this turn, copy it, except the copy isn't legendary.
    sagaChapter(2) {
        effect = CreateDelayedTriggerEffect(
            trigger = Triggers.YouCastCreature,
            effect = Effects.CopyTargetSpell(
                target = EffectTarget.TriggeringEntity,
                removeLegendary = true,
            ),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn,
        )
    }

    // III — Choose a card name. Whenever a creature with the chosen name deals combat damage to a
    // player this turn, draw a card.
    sagaChapter(3) {
        effect = Effects.Composite(
            Effects.ChooseCardName(storeAs = "clonedName"),
            CreateDelayedTriggerEffect(
                trigger = Triggers.dealsDamage(
                    damageType = DamageType.Combat,
                    recipient = RecipientFilter.AnyPlayer,
                    sourceFilter = GameObjectFilter.Creature.namedFromVariable("clonedName"),
                    binding = TriggerBinding.ANY,
                ),
                effect = Effects.DrawCards(1),
                fireOnce = false,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Bill Sienkiewicz"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/976432b3-bc17-4edb-86d6-00fd1baf9670.jpg?1783905356"
    }
}
