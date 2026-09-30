package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invert Polarity
 * {U}{U}{R}
 * Instant
 *
 * Choose target spell, then flip a coin. If you win the flip, gain control of that spell and you
 * may choose new targets for it. If you lose the flip, counter that spell.
 *
 * `GainControl` aimed at a spell rewrites the stack object's controller, so the retarget that
 * follows is judged from your perspective and a stolen permanent spell enters under your control.
 * The retarget is offered only after the flip is won (the first ruling).
 */
val InvertPolarity = card("Invert Polarity") {
    manaCost = "{U}{U}{R}"
    typeLine = "Instant"
    oracleText = "Choose target spell, then flip a coin. If you win the flip, gain control of that " +
        "spell and you may choose new targets for it. If you lose the flip, counter that spell."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.FlipCoin(
            wonEffect = Effects.GainControl(spell) then Effects.ChangeTriggeringObjectTargets(spell = spell),
            lostEffect = Effects.CounterSpell()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Leonardo Santanna"
        flavorText = "\"Skreeg! I thought I told you to stop messing with... Oh, that worked. Good job, Skreeg.\"\n—Ral Zarek"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcee6a8a-c3a8-43bc-beb9-be30d03ab952.jpg?1783911250"
        ruling("2024-06-07", "You don't make any decisions about new targets for the spell until you win the flip.")
        ruling("2024-06-07", "If you choose new targets for the target spell, the new targets must be legal.")
        ruling("2024-06-07", "If the target spell has a variable number of targets, you can't change how many targets it has.")
        ruling("2024-06-07", "If the target spell has damage divided as it was cast, the division can't be changed although the targets receiving that damage still can. The same is true of spells that distribute counters.")
    }
}
