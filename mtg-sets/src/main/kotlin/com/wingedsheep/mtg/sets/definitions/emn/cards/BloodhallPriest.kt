package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/** Bloodhall Priest — Eldritch Moon #181. */
val BloodhallPriest = card("Bloodhall Priest") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Vampire Cleric"
    oracleText = "Whenever this creature enters or attacks, if you have no cards in hand, this " +
        "creature deals 2 damage to any target.\n" +
        "Madness {1}{B}{R} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.EmptyHand
        val damageTarget = target("any target", Targets.Any)
        effect = ConditionalEffect(
            condition = Conditions.EmptyHand,
            effect = Effects.DealDamage(2, damageTarget),
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.EmptyHand
        val damageTarget = target("any target", Targets.Any)
        effect = ConditionalEffect(
            condition = Conditions.EmptyHand,
            effect = Effects.DealDamage(2, damageTarget),
        )
    }

    madness("{1}{B}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4824cca-0039-4486-be8f-650dac2c8e9f.jpg?1783937434"
        ruling("2025-01-24", "If you have a card in hand at the moment the trigger condition occurs, Bloodhall Priest's ability won't trigger. If you have a card in hand as the ability resolves, no damage will be dealt.")
    }
}
