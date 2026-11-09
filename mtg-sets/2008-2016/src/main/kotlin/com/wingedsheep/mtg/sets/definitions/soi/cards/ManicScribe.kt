package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Manic Scribe (Shadows over Innistrad #73)
 * {1}{U}
 * Creature — Human Wizard
 * 0/3
 *
 * When this creature enters, each opponent mills three cards.
 * Delirium — At the beginning of each opponent's upkeep, if there are four or more card types among
 * cards in your graveyard, that player mills three cards.
 *
 * The delirium trigger is an intervening "if": checked as the opponent's upkeep begins and again on
 * resolution. "That player" is the opponent whose upkeep it is — [Player.TriggeringPlayer], bound by
 * the step trigger — so in multiplayer only the active opponent mills.
 */
val ManicScribe = card("Manic Scribe") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 3
    oracleText = "When this creature enters, each opponent mills three cards.\n" +
        "Delirium — At the beginning of each opponent's upkeep, if there are four or more card types " +
        "among cards in your graveyard, that player mills three cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.Delirium()
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Delirium — At the beginning of each opponent's upkeep, if there are four or more " +
            "card types among cards in your graveyard, that player mills three cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb0fadba-4205-47ce-b4cf-e0ff7308749b.jpg?1783937793"
        ruling(
            "2016-04-08",
            "Manic Scribe's delirium ability mills an opponent's library before that player draws a card " +
                "during their draw step."
        )
        ruling(
            "2016-04-08",
            "There must be four or more card types among cards in your graveyard for the delirium ability " +
                "to trigger. The number of card types is checked again as it resolves, and if it has become " +
                "too low, the ability does nothing."
        )
    }
}
