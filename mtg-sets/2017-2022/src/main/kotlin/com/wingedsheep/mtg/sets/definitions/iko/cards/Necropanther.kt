package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Necropanther
 * {1}{W}{B}
 * Creature — Cat Nightmare
 * 3/3
 * Mutate {2}{W/B}{W/B}
 * Whenever this creature mutates, return target creature card with mana value 3 or less from your
 * graveyard to the battlefield.
 */
val Necropanther = card("Necropanther") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Cat Nightmare"
    power = 3
    toughness = 3
    oracleText = "Mutate {2}{W/B}{W/B} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, return target creature card with mana value 3 or less from your graveyard to the battlefield."

    keywordAbility(KeywordAbility.mutate("{2}{W/B}{W/B}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val creatureCard = target(
            TargetFilter(GameObjectFilter.Creature.ownedByYou().manaValueAtMost(3), zone = Zone.GRAVEYARD),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e1ed055-8988-4625-9d57-8ce8a4e04aea.jpg?1783931022"
    }
}
