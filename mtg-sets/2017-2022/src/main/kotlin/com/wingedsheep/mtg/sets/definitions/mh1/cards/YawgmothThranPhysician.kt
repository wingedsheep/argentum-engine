package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Yawgmoth, Thran Physician
 * {2}{B}{B}
 * Legendary Creature — Human Cleric
 * 2/4
 * Protection from Humans
 * Pay 1 life, Sacrifice another creature: Put a -1/-1 counter on up to one target creature and
 * draw a card.
 * {B}{B}, Discard a card: Proliferate.
 *
 * The first ability may be activated with no target (you just draw); if a chosen target becomes
 * illegal the whole ability fizzles and no card is drawn.
 */
val YawgmothThranPhysician = card("Yawgmoth, Thran Physician") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 4
    oracleText = "Protection from Humans\n" +
        "Pay 1 life, Sacrifice another creature: Put a -1/-1 counter on up to one target creature and draw a card.\n" +
        "{B}{B}, Discard a card: Proliferate. (Choose any number of permanents and/or players, then give each " +
        "another counter of each kind already there.)"

    keywordAbility(KeywordAbility.protectionFromSubtype("Human"))

    activatedAbility {
        cost = Costs.Composite(Costs.PayLife(1), Costs.SacrificeAnother(GameObjectFilter.Creature))
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature) then
            Effects.DrawCards(1)
        description = "Pay 1 life, Sacrifice another creature: Put a -1/-1 counter on up to one target " +
            "creature and draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}{B}"), Costs.DiscardCard)
        effect = Effects.Proliferate()
        description = "{B}{B}, Discard a card: Proliferate."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "116"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8690cbcc-f8fd-41f7-9e28-e61c12b04014.jpg?1783933118"
        ruling("2021-03-19", "Protection from Humans refers only to the creature type Human. As far as Yawgmoth is concerned, you, your opponents, and planeswalkers aren't Humans.")
        ruling("2021-03-19", "You may activate Yawgmoth's first activated ability without choosing a target creature. You'll just draw a card. However, if you choose a target and it becomes illegal before the ability tries to resolve, the ability won't resolve and you won't draw a card.")
    }
}
