package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Send to Sleep
 * {1}{U}
 * Instant
 *
 * Tap up to two target creatures.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * those creatures don't untap during their controllers' next untap steps.
 *
 * The Frost Breath shape with the DOESNT_UNTAP grant gated on spell mastery: each still-legal
 * target is tapped and, if the resolution-time [Conditions.CardsInGraveyardMatchingAtLeast] check
 * holds, skips its *own* controller's next untap step. Send to Sleep is still on the stack during
 * that check, so it never counts toward spell mastery.
 */
val SendToSleep = card("Send to Sleep") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "those creatures don't untap during their controllers' next untap steps."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.If(
                condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                then = Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    EffectTarget.ContextTarget(0),
                    Duration.UntilAfterAffectedControllersNextUntap()
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa34d648-da0a-4615-8dd8-d2c8b1a1a574.jpg?1783938349"
        ruling("2015-06-22", "If the spell mastery ability applies and a creature affected by Send to Sleep changes controllers before its old controller's next untap step, Send to Sleep will prevent it from becoming untapped during its new controller's next untap step.")
        ruling("2015-06-22", "Send to Sleep can target tapped creatures. If a targeted creature is already tapped when the spell resolves (and the spell mastery ability applies), that creature remains tapped and doesn't untap during its controller's next untap step.")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
