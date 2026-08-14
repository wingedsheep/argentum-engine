package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sound the Trumpets
 * {1}{U}{U}
 * Instant
 *
 * Counter target spell. If that spell's mana value was 2 or less, recruit.
 *
 * The mana-value test is deliberately hoisted *above* the counter rather than sequenced after it
 * ([ConditionalEffect] wrapping both branches, Prohibit's shape) so it reads the spell while it is
 * still on the stack. Two things would go wrong reading it afterwards: an X spell's mana value
 * counts its chosen X only on the stack (CR 202.3b) and drops to X=0 once the card is in the
 * graveyard, and the check would then be evaluating a card, not the spell the oracle text refers to
 * in the past tense ("was 2 or less"). Ordering is unchanged from the printed text — the counter
 * still happens before the recruit, and no player gets priority in between.
 *
 * The else branch repeats the bare counter because the condition gates only the recruit rider: a
 * mana value of 3 or more still gets countered, it just mints nothing. A spell that can't be
 * countered stays on the stack (the engine's counter is a no-op there), but the recruit rider is
 * keyed to the mana value, not to the counter succeeding, so it still happens — matching CR, since
 * "counter target spell" that fails to counter does not stop the rest of the resolution.
 */
val SoundTheTrumpets = card("Sound the Trumpets") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. If that spell's mana value was 2 or less, recruit. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, create a 1/1 white " +
        "Human Soldier creature token.)"

    spell {
        target = Targets.Spell
        effect = ConditionalEffect(
            condition = Conditions.TargetSpellManaValueAtMost(DynamicAmount.Fixed(2)),
            effect = Effects.Composite(
                Effects.CounterSpell(),
                Patterns.Mechanic.recruit(),
            ),
            elseEffect = Effects.CounterSpell(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Nereida"
        flavorText = "The warning trumpets were suddenly sounded, and echoed along the rocky " +
            "shores. So it was that the Dragon did not find them quite unprepared."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd32a1dd-3541-4572-a717-1deabc14b827.jpg?1784760160"
    }
}
