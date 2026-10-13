package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Djinn of the Fountain — Dominaria United #47
 * {4}{U}{U} · Creature — Djinn · Uncommon · 4/4
 *
 * Flying
 * Whenever you cast an instant or sorcery spell, choose one —
 * • This creature gets +1/+1 until end of turn.
 * • Exile this creature. Return it to the battlefield under its owner's control at the beginning
 *   of the next end step.
 * • Scry 1.
 *
 * A [ModalEffect.chooseOne] off `Triggers.you.casts(GameObjectFilter.InstantOrSorcery)` — the
 * Judith, Carnage Connoisseur shape. The self-blink mode is the Fleeting Spirit composition:
 * move this creature to exile, then a delayed trigger at [Step.END] returns it to the
 * battlefield (as a new object, under its owner's control).
 */
val DjinnOfTheFountain = card("Djinn of the Fountain") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you cast an instant or sorcery spell, choose one —\n" +
        "• This creature gets +1/+1 until end of turn.\n" +
        "• Exile this creature. Return it to the battlefield under its owner's control at the " +
        "beginning of the next end step.\n" +
        "• Scry 1."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.ModifyStats(1, 1, EffectTarget.Self),
                "This creature gets +1/+1 until end of turn"
            ),
            Mode.noTarget(
                Effects.Move(EffectTarget.Self, Zone.EXILE) then
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD)
                    ),
                "Exile this creature. Return it to the battlefield under its owner's control " +
                    "at the beginning of the next end step"
            ),
            Mode.noTarget(
                Effects.Scry(1),
                "Scry 1"
            )
        )
        description = "Whenever you cast an instant or sorcery spell, choose one — this creature " +
            "gets +1/+1 until end of turn; or exile this creature and return it to the battlefield " +
            "under its owner's control at the beginning of the next end step; or scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f55606da-de3e-4375-925b-cc16b03b6365.jpg?1783921354"

        ruling(
            "2022-09-09",
            "Djinn of the Fountain's triggered ability will resolve before the instant or sorcery " +
                "spell that caused it to trigger resolves. It will resolve even if the spell that " +
                "caused it to trigger is countered or has otherwise left the stack."
        )
    }
}
