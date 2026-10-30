package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kroxa, Titan of Death's Hunger
 * {B}{R}
 * Legendary Creature — Elder Giant
 * 6/6
 * When Kroxa enters, sacrifice it unless it escaped.
 * Whenever Kroxa enters or attacks, each opponent discards a card, then each opponent who didn't
 * discard a nonland card this way loses 3 life.
 * Escape—{B}{B}{R}{R}, Exile five other cards from your graveyard.
 *
 * The sacrifice is Phlage's shape: an [Effects.If] over [Conditions.Escaped], checked as the trigger
 * resolves. The discard follows the rulings' procedure: each opponent in turn order picks a card
 * from their own hand (nothing leaves a hand yet), and while choosing is recorded as a loser unless
 * the pick is a nonland card — an empty hand records them too ("or if they can't discard a card at
 * all"). Then every chosen card is discarded at once, and only after that do the recorded opponents
 * lose 3 life. Per-opponent tracking comes from [forEachPlayerCollecting]'s fresh scope per player.
 */
private val discardOrLoseThree: Effect = Effects.Pipeline(
    descriptionOverride = "Each opponent discards a card, then each opponent who didn't discard a " +
        "nonland card this way loses 3 life."
) {
    val (chosen, losers) = forEachPlayerCollecting(Player.EachOpponent) {
        val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
        val pick = chooseExactly(1, hand, prompt = "Choose a card to discard")
        listOf(pick, storePlayer(onlyIf = Conditions.Not(whenMatches(pick, GameObjectFilter.Nonland))))
    }
    discard(chosen)
    run(Effects.ForEachPlayer(losers.asPlayers, Effects.LoseLife(3, EffectTarget.Controller)))
}

val KroxaTitanOfDeathsHunger = card("Kroxa, Titan of Death's Hunger") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Elder Giant"
    power = 6
    toughness = 6
    oracleText = "When Kroxa enters, sacrifice it unless it escaped.\n" +
        "Whenever Kroxa enters or attacks, each opponent discards a card, then each opponent who didn't " +
        "discard a nonland card this way loses 3 life.\n" +
        "Escape—{B}{B}{R}{R}, Exile five other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(Conditions.Not(Conditions.Escaped), SacrificeSelfEffect)
        description = "When Kroxa enters, sacrifice it unless it escaped."
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = discardOrLoseThree
        description = "Whenever Kroxa enters, each opponent discards a card, then each opponent who " +
            "didn't discard a nonland card this way loses 3 life."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = discardOrLoseThree
        description = "Whenever Kroxa attacks, each opponent discards a card, then each opponent who " +
            "didn't discard a nonland card this way loses 3 life."
    }

    keywordAbility(KeywordAbility.escape("{B}{B}{R}{R}", Costs.additional.ExileOtherCards(5)))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "221"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cee0459b-9aac-4d2f-abe4-4d5fedde7eb8.jpg?1783931520"
        ruling(
            "2020-01-24",
            "Kroxa's first ability causes you to sacrifice it if you didn't cast it, or if it was cast using " +
                "any permission other than an escape ability."
        )
        ruling("2020-01-24", "Kroxa's second ability triggers when it enters the battlefield, even if it didn't escape.")
        ruling(
            "2020-01-24",
            "As the second ability resolves, first the next opponent in turn order (or, if it's an opponent's " +
                "turn, the opponent whose turn it is) chooses a card in hand without revealing it, then each " +
                "other opponent in turn order does the same. All the chosen cards are discarded at the same " +
                "time, and then the appropriate players lose 3 life each."
        )
        ruling("2020-01-24", "An opponent loses 3 life if they discard a land card or if they can't discard a card at all.")
    }
}
