package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * All-Seeing Arbiter
 * {4}{U}{U}
 * Creature — Avatar
 * 5/4
 *
 * Flying
 * Whenever this creature enters or attacks, draw two cards, then discard a card.
 * Whenever you discard a card, target creature an opponent controls gets -X/-0 until your next
 * turn, where X is the number of different mana values among cards in your graveyard.
 *
 * "Enters or attacks" is two triggered abilities sharing one effect. X is read once, as the
 * discard trigger resolves (CR 608.2h), and locked into the -X/-0 modification.
 */
val AllSeeingArbiter = card("All-Seeing Arbiter") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Avatar"
    power = 5
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever this creature enters or attacks, draw two cards, then discard a card.\n" +
        "Whenever you discard a card, target creature an opponent controls gets -X/-0 until your next turn, where X is the number of different mana values among cards in your graveyard."

    keywords(Keyword.FLYING)

    val drawTwoDiscardOne: Effect = Effects.DrawCards(2) then Effects.Discard(1)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = drawTwoDiscardOne
        description = "Whenever this creature enters, draw two cards, then discard a card."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = drawTwoDiscardOne
        description = "Whenever this creature attacks, draw two cards, then discard a card."
    }

    triggeredAbility {
        trigger = Triggers.you.discards()
        val victim = target(TargetFilter.CreatureOpponentControls)
        val manaValues = DynamicAmounts.zone(Player.You, Zone.GRAVEYARD)
            .distinctValues(CardNumericProperty.MANA_VALUE)
        effect = Effects.ModifyStats(
            power = -manaValues,
            toughness = DynamicAmounts.fixed(0),
            target = victim,
            duration = Duration.UntilYourNextTurn
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "34"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c832c0b8-aefa-459c-8de8-aa32400612cd.jpg?1783923151"
        ruling("2022-04-29", "The mana value of a split card in the graveyard is the total mana value of both halves of that card. It does not have two mana values.")
        ruling("2022-04-29", "The mana value of a double-faced card in your graveyard is always the mana value of the front face.")
        ruling("2022-04-29", "X is 0 when determining the mana value of a card in your graveyard.")
        ruling("2022-04-29", "The mana value of a land card is 0.")
    }
}
