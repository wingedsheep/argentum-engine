package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flummoxed Cyclops
 * {3}{R}
 * Creature — Cyclops
 * 4/4
 * Reach
 * Whenever two or more creatures your opponents control attack, this creature can't block this
 * combat.
 *
 * An opponent's attack declaration of two or more creatures ([Triggers.anOpponent] `attacks`
 * with `minAttackers = 2`) makes the Cyclops unable to block until end of combat. The count is
 * fixed at declaration, so losing attackers before resolution doesn't undo it (ruling).
 */
val FlummoxedCyclops = card("Flummoxed Cyclops") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cyclops"
    power = 4
    toughness = 4
    oracleText = "Reach\n" +
        "Whenever two or more creatures your opponents control attack, this creature can't block this combat."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.anOpponent.attacks(minAttackers = 2)
        effect = Effects.CantBlock(EffectTarget.Self, Duration.EndOfCombat)
        description = "Whenever two or more creatures your opponents control attack, this creature " +
            "can't block this combat."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Mark Behm"
        flavorText = "\"A cyclops thinks slowly but strikes quickly. Try to keep it thinking.\"\n" +
            "—Brigone, soldier of Meletis"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f70bcef-7ab6-4547-aca4-dd3ba68fd26b.jpg?1783931553"
        ruling(
            "2020-01-24",
            "Once two or more creatures your opponents control have attacked, Flummoxed Cyclops won't be " +
                "able to block, even if all but one of those attackers leave the battlefield before its " +
                "triggered ability resolves."
        )
    }
}
