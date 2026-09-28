package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Kamigawa // Rooftop Saboteurs — March of the Machine #62.
 * {3}{U} · Battle — Siege · defense 4 // Creature — Moonfolk Ninja, 2/3
 *
 * When this Siege enters, tap target artifact or creature an opponent controls and put a stun
 * counter on it. (Waylaying Pirates' tap-and-stun pair.)
 * // Flying; whenever this creature deals combat damage to a player or battle, draw a card.
 */
private val InvasionOfKamigawaFront = card("Invasion of Kamigawa") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, tap target artifact or creature an opponent controls and put a " +
        "stun counter on it. (If a permanent with a stun counter would become untapped, remove " +
        "one from it instead.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls()))
        effect = Effects.Tap(t) then
            Effects.AddCounters(counterType = CounterType.STUN, count = 1, target = t)
        description = "When this Siege enters, tap target artifact or creature an opponent controls " +
            "and put a stun counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3140320d-b932-441f-bff3-9bdf4419b88a.jpg?1783917040"
    }
}

private val RooftopSaboteurs = card("Rooftop Saboteurs") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Creature — Moonfolk Ninja"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player or battle, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
        effect = Effects.DrawCards(1)
        description = "Whenever this creature deals combat damage to a player or battle, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Kekai Kotaki"
        flavorText = "The pace of Kamigawa's development meant that Jin-Gitaxias's intel was " +
            "already outdated when the invaders arrived."
        imageUri = "https://cards.scryfall.io/normal/back/3/1/3140320d-b932-441f-bff3-9bdf4419b88a.jpg?1783917040"
    }
}

val InvasionOfKamigawa: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfKamigawaFront,
    backFace = RooftopSaboteurs,
)
