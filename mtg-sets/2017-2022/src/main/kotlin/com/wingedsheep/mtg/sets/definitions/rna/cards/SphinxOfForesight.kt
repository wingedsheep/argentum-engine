package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.revealFromOpeningHand
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sphinx of Foresight — Ravnica Allegiance #55
 * {2}{U}{U} · Creature — Sphinx · 4 / 4
 *
 * The opening-hand reveal (CR 103.6b) follows Devourer of Destiny: the payoff is a delayed
 * trigger created before turn 1 that fires at the controller's first upkeep.
 */
val SphinxOfForesight = card("Sphinx of Foresight") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 4
    toughness = 4
    oracleText = "You may reveal this card from your opening hand. If you do, scry 3 at the beginning of " +
        "your first upkeep.\n" +
        "Flying\n" +
        "At the beginning of your upkeep, scry 1."

    revealFromOpeningHand(
        Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            effect = Effects.Scry(3)
        )
    )

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Titus Lunter"
        flavorText = "\"I do not understand the future I see: the guilds subverted, the world at war, armies " +
            "bent to a cruel conqueror's will.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf2386fd-edc0-4731-8f4e-7a7c45548bf3.jpg?1783933703"
        ruling(
            "2021-03-19",
            "A player's \"opening hand\" is the hand of cards the player has after all players have taken " +
                "mulligans. If players have any cards in hand that allow actions to be taken with them from a " +
                "player's opening hand, the starting player takes all such actions first in any order, followed " +
                "by each other player in turn order. Then the first turn begins."
        )
        ruling(
            "2019-01-25",
            "If you reveal two Sphinxes of Foresight from your opening hand, you'll scry 3 twice; you won't " +
                "scry 6. Any cards you put on top of your library the first time you scry 3 will be part of the " +
                "second time you scry 3."
        )
    }
}
