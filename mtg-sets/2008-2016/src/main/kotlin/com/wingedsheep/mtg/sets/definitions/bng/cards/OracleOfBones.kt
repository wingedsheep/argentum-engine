package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Oracle of Bones
 * {2}{R}{R}
 * Creature — Minotaur Shaman
 * 3/1
 *
 * Haste
 * Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, you may cast an instant or sorcery spell from your
 * hand without paying its mana cost.
 *
 * Canonical printing: Born of the Gods, the card's only printing.
 */
val OracleOfBones = card("Oracle of Bones") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Shaman"
    power = 3
    toughness = 1
    oracleText = "Haste\n" +
        "Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, you may cast an instant or sorcery spell from your hand without paying its mana cost."

    keywords(Keyword.HASTE)

    tribute(2)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val spellToCast = chooseSpell(
                from = hand,
                filter = GameObjectFilter.InstantOrSorcery,
                showAllCards = true,
                prompt = "You may cast an instant or sorcery spell from your hand without paying its mana cost",
                selectedLabel = "Cast for free"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(spellToCast))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddb1931e-c282-47a7-9382-608de6ec8efa.jpg?1783939544"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the card has {X} in its mana cost, you must choose 0 as its value.")
        ruling("2014-02-01", "If you cast a card \"without paying its mana cost,\" you can't pay alternative costs such as overload costs. You can, however, pay additional costs such as kicker costs. If the card has mandatory additional costs, you must pay those.")
        ruling("2014-02-01", "If you choose to cast a split card with fuse without paying its mana cost, you may cast both halves.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "If you want to cast a card this way, you cast it as part of the resolution of the triggered ability. Timing restrictions based on the card's type (such as sorcery) are ignored. Other casting restrictions (such as \"Cast [this card] only before attackers are declared\") are not.")
        ruling("2014-02-01", "If the triggered ability has a target, that target will not be known while the creature spell with tribute is on the stack.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
        ruling("2014-02-01", "The choice of whether to pay tribute is made as the creature with tribute is entering the battlefield. At that point, it's too late to respond to the creature spell. For example, in a multiplayer game, opponents won't know whether tribute will be paid or which opponent will be chosen to pay tribute or not when deciding whether to counter the creature spell.")
    }
}
