package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Code of Constraint
 * {2}{U}
 * Instant
 * Target creature gets -4/-0 until end of turn.
 * Draw a card.
 * Addendum — If you cast this spell during your main phase, tap that creature and it doesn't
 * untap during its controller's next untap step.
 *
 * Addendum is checked at resolution: [Conditions.WasCast] excludes copies (never cast), and
 * [Conditions.IsYourMainPhase] reads the cast-time phase, since a phase only ends with an empty stack.
 */
val CodeOfConstraint = card("Code of Constraint") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-0 until end of turn.\n" +
        "Draw a card.\n" +
        "Addendum — If you cast this spell during your main phase, tap that creature and it doesn't untap during its controller's next untap step."

    spell {
        val creature = target(TargetFilter.Creature)

        effect = Effects.ModifyStats(-4, 0, creature) then
            Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase),
                then = Effects.Tap(creature) then Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    creature,
                    Duration.UntilAfterAffectedControllersNextUntap(),
                ),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/258aeef1-565a-4f19-b12c-d46d54ba231d.jpg?1783933710"
    }
}
