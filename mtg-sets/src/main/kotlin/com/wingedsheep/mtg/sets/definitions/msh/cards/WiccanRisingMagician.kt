package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Wiccan, Rising Magician
 * {4}{U}
 * Legendary Creature — Mutant Warlock Hero
 * 4/4
 *
 * Flying
 * Whenever you cast a noncreature spell, exile another target nonland, nontoken permanent. Return
 * that card to the battlefield under its owner's control at the beginning of the next end step.
 *
 * The blink is the Skyskipper Duo shape: [Effects.Move] to exile plus a [CreateDelayedTriggerEffect]
 * on [Step.END] that moves the same bound target back. The return carries no controller override, so
 * the card comes back under its *owner's* control — which matters here because, unlike Skyskipper
 * Duo, the target isn't restricted to permanents you control.
 *
 * The target is "another nonland, nontoken permanent": a battlefield [TargetPermanent] over
 * `NonlandPermanent.nontoken()` with `.other()` so Wiccan can't blink itself. Tokens are excluded
 * because a blinked token would simply cease to exist (CR 111.7); the printed wording keeps them
 * off the table entirely rather than making the return a no-op.
 */
val WiccanRisingMagician = card("Wiccan, Rising Magician") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Mutant Warlock Hero"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you cast a noncreature spell, exile another target nonland, nontoken permanent. " +
        "Return that card to the battlefield under its owner's control at the beginning of the " +
        "next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        val permanent = target(
            "another target nonland, nontoken permanent",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.NonlandPermanent.nontoken()).other(),
            ),
        )
        effect = Effects.Composite(
            Effects.Move(permanent, Zone.EXILE),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = Effects.Move(permanent, Zone.BATTLEFIELD),
            ),
        )
        description = "Whenever you cast a noncreature spell, exile another target nonland, " +
            "nontoken permanent. Return that card to the battlefield under its owner's control " +
            "at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Josu Hernaiz"
        flavorText = "\"Step outside of reality for a moment. It might improve your attitude.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d27ddec-716d-47e5-ac48-365e91b88ff1.jpg?1783902949"
    }
}
