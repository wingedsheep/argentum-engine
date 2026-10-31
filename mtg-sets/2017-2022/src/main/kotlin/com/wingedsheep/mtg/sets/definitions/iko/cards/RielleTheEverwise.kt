package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rielle, the Everwise
 * {1}{U}{R}
 * Legendary Creature — Human Wizard
 * 0/3
 * Rielle gets +1/+0 for each instant and sorcery card in your graveyard.
 * Whenever you discard one or more cards for the first time each turn, draw that many cards.
 *
 * The draw trigger is the batch discard trigger with the `firstTimeEachTurn` turn-history window
 * (Veronica, Dissident Scribe's shape): it fires once, for the turn's first discard event, and a
 * discard earlier in the turn closes the window even if Rielle wasn't on the battlefield then.
 * "That many" is that event's card count (`triggerDiscardCount`, as on Cryptcaller Chariot).
 */
val RielleTheEverwise = card("Rielle, the Everwise") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Wizard"
    power = 0
    toughness = 3
    oracleText = "Rielle gets +1/+0 for each instant and sorcery card in your graveyard.\n" +
        "Whenever you discard one or more cards for the first time each turn, draw that many cards."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.zone(
                Player.You,
                Zone.GRAVEYARD,
                GameObjectFilter.InstantOrSorcery
            ).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.discards(batch = true, firstTimeEachTurn = true)
        effect = Effects.DrawCards(DynamicAmounts.triggerDiscardCount())
        description = "Whenever you discard one or more cards for the first time each turn, draw that many cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "203"
        artist = "Yongjae Choi"
        flavorText = "\"Being all-knowing is a matter of remembering which things it is crucial to forget.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f03c944-1929-4cb2-a373-d57eefa29ed1.jpg?1783931018"
        ruling("2020-04-17", "Rielle's first ability applies only while it's on the battlefield.")
        ruling(
            "2020-04-17",
            "If you discard a card as a cost to cast a spell or activate an ability, Rielle's triggered " +
                "ability resolves before that spell or ability but after you've chosen targets for it. If you " +
                "discard a card while a spell or ability is resolving, that spell or ability finishes " +
                "resolving before Rielle's triggered ability does."
        )
        ruling(
            "2020-04-17",
            "If the first time you discard cards during your turn is due to having too many cards in hand " +
                "during your cleanup step, you'll draw that many cards. Players will receive priority during " +
                "the cleanup step in this case, and a new cleanup step will happen after that one, during " +
                "which you may have to discard cards again."
        )
    }
}
