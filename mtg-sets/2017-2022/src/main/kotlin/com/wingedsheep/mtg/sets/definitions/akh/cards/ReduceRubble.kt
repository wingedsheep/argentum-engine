package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reduce // Rubble — Amonkhet #216
 *
 * Reduce {2}{U} — Instant: Counter target spell unless its controller pays {3}.
 * Rubble {2}{R} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Up to three target lands don't untap during their controller's next untap step.
 *
 * Rubble doesn't tap anything; each chosen land gets DOESNT_UNTAP keyed to its *own* controller's
 * next untap step (the Frost Breath shape, minus the tap).
 */
val ReduceRubble = card("Reduce // Rubble") {
    layout = CardLayout.SPLIT
    colorIdentity = "UR"

    face("Reduce") {
        manaCost = "{2}{U}"
        typeLine = "Instant"
        oracleText = "Counter target spell unless its controller pays {3}."

        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterUnlessPays("{3}")
        }
    }

    face("Rubble") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Up to three target lands don't untap during their controller's next untap step."
        keywords(Keyword.AFTERMATH)

        spell {
            targets(TargetFilter.Land, count = 3, optional = true)
            effect = Effects.ForEachTarget(
                Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    EffectTarget.ContextTarget(0),
                    Duration.UntilAfterAffectedControllersNextUntap()
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2f3035c-ca27-40f3-ad73-c4e54bb2bcd7.jpg?1783936457"
        ruling("2017-07-14", "Once you've started to cast a spell with aftermath from your graveyard, the card is immediately moved to the stack. Opponents can't try to stop the ability by exiling the card with an effect such as that of Crook of Condemnation.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
        ruling("2017-04-18", "If you cast the first half of a split card with aftermath during your turn, you'll have priority immediately after it resolves. You can cast the half with aftermath from your graveyard before any player can take any other action if it's legal for you to do so.")
    }
}
