package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rubblebelt Braggart — Murders at Karlov Manor #143
 * {4}{R} · Creature — Lizard Warrior · 5/5
 *
 * Whenever this creature attacks, if it's not suspected, you may suspect it.
 *
 * "if it's not suspected" is an **intervening-if** (CR 603.4), modelled as
 * `triggerCondition = Conditions.Not(Conditions.SourceIsSuspected)` — which reads the projected
 * suspected designation rather than probing for menace. An already-suspected Braggart never puts
 * the trigger on the stack at all, so the player isn't asked a question that couldn't do anything.
 *
 * The engine checks `triggerCondition` at trigger time but not again at resolution, which CR 603.4
 * also requires. That gap is inert here: suspect is idempotent by CR 701.60d ("a suspected
 * permanent can't become suspected again"), and `SetSuspectedExecutor` enforces exactly that, so a
 * Braggart that somehow became suspected between trigger and resolution just resolves to a no-op.
 *
 * "Suspect it" names the source, not a target, so it is `EffectTarget.Self`; the "you may" is a
 * plain yes/no decision on resolution. Saying yes is a real cost — the Braggart gains menace but
 * permanently loses the ability to block, and there is no way to shed the designation later.
 */
val RubblebeltBraggart = card("Rubblebelt Braggart") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Warrior"
    oracleText = "Whenever this creature attacks, if it's not suspected, you may suspect it. " +
        "(A suspected creature has menace and can't block.)"
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.Not(Conditions.SourceIsSuspected)
        // `Effects.Suspect`'s own description is the sentence "this creature becomes suspected",
        // which reads as gibberish under the gate's "You may …" prefix. Spell the question out so
        // the yes/no prompt names the trade the player is actually making.
        effect = MayEffect(
            Effects.Suspect(EffectTarget.Self),
            descriptionOverride = "Suspect Rubblebelt Braggart? (It gains menace but can't block.)"
        )
        description = "Whenever this creature attacks, if it's not suspected, you may suspect it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Leonardo Santanna"
        flavorText = "\"I did it! Double—no, triple homicide! In a building I set on fire. Yep, all me.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f90f8691-210a-4bf0-9fc2-fb2efcf057fb.jpg?1783912875"

        ruling("2024-02-02", "If a creature is already suspected, suspecting it again won't have any effect.")
        ruling(
            "2024-02-02",
            "If a suspected creature loses all abilities, it will lose menace and \"This creature " +
                "can't block\", but it won't stop being suspected."
        )
        ruling(
            "2024-02-02",
            "Being suspected isn't a copiable value. If a permanent becomes a copy of a suspected " +
                "creature, it won't be suspected."
        )
    }
}
