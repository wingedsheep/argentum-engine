package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Minion of the Mighty
 * {R}
 * Creature — Kobold
 * 0/1
 * Menace
 * Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power 6
 * or greater this combat, you may put a Dragon creature card from your hand onto the battlefield
 * tapped and attacking.
 */
val MinionOfTheMighty = card("Minion of the Mighty") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kobold"
    oracleText = "Menace\nPack tactics — Whenever this creature attacks, if you attacked with creatures with " +
        "total power 6 or greater this combat, you may put a Dragon creature card from your hand onto the " +
        "battlefield tapped and attacking."
    power = 0
    toughness = 1

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackersTotalPowerAtLeast(6)))
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature.withSubtype("Dragon"),
            entersAttacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Oriana Menendez"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffd0a827-778b-48c3-bb85-4b4acef351d6.jpg?1783926474"

        ruling("2021-07-23", "A Dragon creature card is a creature card with the creature type Dragon, not necessarily a creature card with the word \"Dragon\" in its name.")
        ruling("2021-07-23", "You choose which player or planeswalker the Dragon is attacking as you put it onto the battlefield. It doesn't have to be the same player or planeswalker Minion of the Mighty is attacking.")
        ruling("2021-07-23", "The creature card is already tapped and attacking as it's put onto the battlefield. Any abilities that trigger whenever a creature becomes tapped or whenever a creature attacks won't trigger.")
    }
}
