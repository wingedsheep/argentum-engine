package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect

/**
 * The Sentry, Golden Guardian
 * {3}{W}
 * Legendary Creature — Human Hero
 * 5/5
 *
 * Flying, vigilance, indestructible
 * When The Sentry enters, target opponent creates The Void, a legendary 5/5 black Horror Villain
 * creature token with flying, indestructible, and "The Void attacks each combat if able."
 *
 * The Void is a *named* token with its own abilities, so it lives in `PredefinedTokens` and is
 * minted through [CreatePredefinedTokenEffect] — the executor reads its type line, color
 * indicator, keywords and the `MustAttack` static off that one registered definition.
 * `controller` is the chosen opponent (a bound player target), the same shape Generous Plunderer
 * uses for "target opponent creates a tapped Treasure token": the token enters under the
 * opponent's control, not the Sentry controller's.
 */
val TheSentryGoldenGuardian = card("The Sentry, Golden Guardian") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Hero"
    power = 5
    toughness = 5
    oracleText = "Flying, vigilance, indestructible\n" +
        "When The Sentry enters, target opponent creates The Void, a legendary 5/5 black Horror " +
        "Villain creature token with flying, indestructible, and \"The Void attacks each combat if able.\""

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        val opponent = target("target opponent", Targets.Opponent)
        trigger = Triggers.EntersBattlefield
        effect = CreatePredefinedTokenEffect("The Void", controller = opponent)
        description = "When The Sentry enters, target opponent creates The Void, a legendary 5/5 " +
            "black Horror Villain creature token with flying, indestructible, and \"The Void " +
            "attacks each combat if able.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Alexander Skripnikov"
        flavorText = "\"For all the kindness I spread, the Void only sows terror.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f56c0e7-5b07-48e3-b0ca-5d09ddc8de9a.jpg?1783902966"
    }
}
