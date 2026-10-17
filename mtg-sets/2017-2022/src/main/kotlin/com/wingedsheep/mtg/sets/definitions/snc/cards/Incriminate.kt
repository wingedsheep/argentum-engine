package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Incriminate — {1}{B}
 * Sorcery (SNC #84, common)
 *
 * Choose two target creatures controlled by the same player. That player sacrifices one of
 * them of their choice.
 *
 * Same shape as Barrin's Spite without the bounce: `sameController` enforces the targeting
 * constraint, and the targets' controller ([Chooser.ControllerOfSelection]) picks which to
 * sacrifice. If only one target is still legal on resolution, it is the one sacrificed.
 */
val Incriminate = card("Incriminate") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose two target creatures controlled by the same player. That player sacrifices one of them of their choice."

    spell {
        targets(TargetFilter.Creature, count = 2, sameController = true)
        effect = Effects.Pipeline {
            val targeted = gather(CardSource.ChosenTargets)
            val sacrificed = chooseExactly(
                1,
                from = targeted,
                chooser = Chooser.ControllerOfSelection,
                useTargetingUI = true,
                prompt = "Choose one of the two creatures to sacrifice"
            )
            sacrifice(sacrificed)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Kieran Yanner"
        flavorText = "\"If I were you, I'd check that elf's coat. I saw him slipping something into his pocket just before you arrived.\"\n—Krandal, freelance instigator"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/198ec497-4e95-4386-9ca4-35d2d3f59733.jpg?1783923130"
    }
}
