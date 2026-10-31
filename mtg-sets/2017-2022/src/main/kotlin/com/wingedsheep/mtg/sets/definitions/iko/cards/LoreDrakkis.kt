package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lore Drakkis
 * {1}{U}{R}
 * Creature — Lizard Beast
 * 2/3
 * Mutate {U/R}{U/R}
 * Whenever this creature mutates, return target instant or sorcery card from your graveyard to your hand.
 */
val LoreDrakkis = card("Lore Drakkis") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Lizard Beast"
    power = 2
    toughness = 3
    oracleText = "Mutate {U/R}{U/R} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, return target instant or sorcery card from your graveyard to your hand."

    keywordAbility(KeywordAbility.mutate("{U/R}{U/R}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val spellCard = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.ReturnToHand(spellCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83e035ca-eccd-4b63-817c-f2c676b9c98d.jpg?1783931022"
    }
}
