package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion

/**
 * Marit Lage's Slumber
 * {1}{U}
 * Legendary Snow Enchantment
 * Whenever Marit Lage's Slumber or another snow permanent you control enters, scry 1.
 * At the beginning of your upkeep, if you control ten or more snow permanents, sacrifice Marit
 * Lage's Slumber. If you do, create Marit Lage, a legendary 20/20 black Avatar creature token with
 * flying and indestructible.
 *
 * The first trigger's subject is "a snow permanent you control" — the Slumber is itself snow, so
 * that one filter covers both "Marit Lage's Slumber" and "another snow permanent".
 *
 * The upkeep trigger is an intervening "if" (CR 603.4), re-checked on resolution, and the token is
 * gated on the sacrifice actually happening: if the Slumber has left the battlefield by then, no
 * permanent is sacrificed and no Marit Lage is created (rulings 2019-06-14).
 */
val MaritLagesSlumber = card("Marit Lage's Slumber") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Snow Enchantment"
    oracleText = "Whenever Marit Lage's Slumber or another snow permanent you control enters, scry 1.\n" +
        "At the beginning of your upkeep, if you control ten or more snow permanents, sacrifice Marit Lage's Slumber. " +
        "If you do, create Marit Lage, a legendary 20/20 black Avatar creature token with flying and indestructible."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.snow().youControl()).enters()
        effect = Effects.Scry(1)
        description = "Whenever Marit Lage's Slumber or another snow permanent you control enters, scry 1."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControlAtLeast(10, GameObjectFilter.Permanent.snow())
        effect = Effects.IfYouDo(
            action = SacrificeSelfEffect,
            then = Effects.CreateToken(
                count = 1,
                power = 20,
                toughness = 20,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Avatar"),
                keywords = setOf(Keyword.FLYING, Keyword.INDESTRUCTIBLE),
                name = "Marit Lage",
                legendary = true,
                imageUri = "https://cards.scryfall.io/normal/front/7/b/7b993828-e139-4cb6-a329-487accc1c515.jpg?1783933228",
            ),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "At the beginning of your upkeep, if you control ten or more snow permanents, " +
            "sacrifice Marit Lage's Slumber. If you do, create Marit Lage, a legendary 20/20 black Avatar " +
            "creature token with flying and indestructible."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f5d2a6e-22cd-4493-9e58-e9c2aa3a31ab.jpg?1783933143"

        ruling("2019-06-14", "If you don't control ten or more snow permanents as your upkeep begins, the last ability won't trigger. No player may take any actions during a turn before the turn's upkeep begins.")
        ruling("2019-06-14", "If you don't control ten snow permanents as the last ability resolves, you can't sacrifice Marit Lage's Slumber.")
        ruling("2019-06-14", "If Marit Lage's Slumber leaves the battlefield before its last ability resolves, you won't be able to sacrifice it, so you won't create Marit Lage.")
    }
}
