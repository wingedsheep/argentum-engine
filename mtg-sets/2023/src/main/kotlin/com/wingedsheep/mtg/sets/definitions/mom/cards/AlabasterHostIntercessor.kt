package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Alabaster Host Intercessor
 * {5}{W}
 * Creature — Phyrexian Samurai
 * 3/4
 * When this creature enters, exile target creature an opponent controls until this creature
 * leaves the battlefield.
 * Plainscycling {2}
 */
val AlabasterHostIntercessor = card("Alabaster Host Intercessor") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Samurai"
    oracleText = "When this creature enters, exile target creature an opponent controls until this creature leaves the battlefield.\nPlainscycling {2} ({2}, Discard this card: Search your library for a Plains card, reveal it, put it into your hand, then shuffle.)"
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ExileUntilLeaves(creature)
    }
    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    keywordAbility(KeywordAbility.typecycling("Plains", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Konstantin Porubov"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/165357cc-ec74-490f-aec3-7048bb43c8f9.jpg?1783917072"
    }
}
