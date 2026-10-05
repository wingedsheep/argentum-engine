package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Valkyrie Harbinger
 * {4}{W}{W}
 * Creature — Angel Cleric
 * 4/5
 * Flying
 * Lifelink
 * At the beginning of each end step, if you gained 4 or more life this turn, create a 4/4 white
 * Angel creature token with flying and vigilance.
 *
 * The token trigger is an intervening-"if": it only triggers if you gained 4 or more life total
 * during the turn before the end step begins, and re-checks on resolution.
 */
val ValkyrieHarbinger = card("Valkyrie Harbinger") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Cleric"
    oracleText = "Flying\n" +
        "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "At the beginning of each end step, if you gained 4 or more life this turn, create a 4/4 " +
        "white Angel creature token with flying and vigilance."
    power = 4
    toughness = 5

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(4)
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/9/8/9864812a-b3b1-4e12-8737-0795cdff994c.jpg?1783902808"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "374"
        artist = "Tran Nguyen"
        flavorText = "Where she flies, the Light of Starnheim shines."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edab83a9-35b5-4312-b8ee-1c042c02aa31.jpg?1783928124"

        ruling("2021-02-05", "You create just one Angel token, no matter how much life you've gained past 4 life.")
        ruling("2021-02-05", "Valkyrie Harbinger's ability looks at how much life you've gained in the turn, even if it wasn't on the battlefield when you gained life. It doesn't care if you also lost life, even if you lost more life than you gained.")
        ruling("2021-02-05", "If you haven't gained 4 life by the time an end step begins, Valkyrie Harbinger's ability won't trigger at all.")
    }
}
