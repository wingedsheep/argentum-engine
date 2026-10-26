package com.wingedsheep.mtg.sets.definitions.onc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Contaminant Grafter
 * {4}{G}
 * Creature — Phyrexian Druid
 * 5/5
 * Trample, toxic 1
 * Whenever one or more creatures you control deal combat damage to one or more players,
 * proliferate.
 * Corrupted — At the beginning of your end step, if an opponent has three or more poison
 * counters, draw a card, then you may put a land card from your hand onto the battlefield.
 *
 * "To one or more players" fires once per combat damage step however many players were hit
 * (`dealCombatDamageToOneOrMorePlayers`), so a two-opponent swing proliferates once, not twice.
 * The land drop's "you may" is `putFromHand`'s choose-up-to-one selection.
 */
val ContaminantGrafter = card("Contaminant Grafter") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Druid"
    power = 5
    toughness = 5
    oracleText = "Trample, toxic 1\n" +
        "Whenever one or more creatures you control deal combat damage to one or more players, proliferate.\n" +
        "Corrupted — At the beginning of your end step, if an opponent has three or more poison counters, " +
        "draw a card, then you may put a land card from your hand onto the battlefield."

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToOneOrMorePlayers()
        effect = Effects.Proliferate()
        description = "Whenever one or more creatures you control deal combat damage to one or more players, proliferate."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Corrupted
        effect = Effects.DrawCards(1) then Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
        description = "Corrupted — At the beginning of your end step, if an opponent has three or more poison " +
            "counters, draw a card, then you may put a land card from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ef2f776-ff35-464b-bfcf-075c9ae4baa7.jpg?1783918158"
        ruling(
            "2023-02-04",
            "Toxic doesn't change the amount of combat damage a creature deals. For example, if a 2/2 creature " +
                "with toxic 1 deals combat damage to a player, that creature will deal 2 damage. The results of " +
                "that damage are the player loses 2 life and gets a poison counter."
        )
        ruling(
            "2023-02-04",
            "Multiple instances of toxic are cumulative. For example, if a creature has toxic 2 and gains toxic 1 " +
                "due to another effect, combat damage that creature deals to a player will cause that player to " +
                "get 3 poison counters."
        )
    }
}
