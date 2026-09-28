package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Etali, Primal Conqueror // Etali, Primal Sickness (March of the Machine #137)
 * {5}{R}{R} Legendary Creature — Elder Dinosaur 7/7 // Legendary Creature — Phyrexian Elder Dinosaur 11/11
 *
 * The ETB walks *every* library (`gatherUntilMatch(player = Player.Each)`), one nonland per library,
 * exiles everything revealed, then casts any number of the nonlands for free during resolution.
 */
private val EtaliPrimalConquerorFront = card("Etali, Primal Conqueror") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "GR"
    typeLine = "Legendary Creature — Elder Dinosaur"
    power = 7
    toughness = 7
    oracleText = "Trample\n" +
        "When Etali enters, each player exiles cards from the top of their library until they exile a " +
        "nonland card. You may cast any number of spells from among the nonland cards exiled this way " +
        "without paying their mana costs.\n" +
        "{9}{G/P}: Transform Etali. Activate only as a sorcery. ({G/P} can be paid with either {G} or 2 life.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val (nonlands, revealed) = gatherUntilMatch(GameObjectFilter.Nonland, player = Player.Each)
            exile(revealed)
            run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(nonlands))
        }
        description = "When Etali enters, each player exiles cards from the top of their library until " +
            "they exile a nonland card. You may cast any number of spells from among the nonland cards " +
            "exiled this way without paying their mana costs."
    }

    activatedAbility {
        cost = Costs.Mana("{9}{G/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Etali."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95c14c4d-6c16-4826-8d93-d89ad04aee09.jpg?1783916997"
    }
}

private val EtaliPrimalSickness = card("Etali, Primal Sickness") {
    manaCost = ""
    colorIndicator = "GR"
    colorIdentity = "GR"
    typeLine = "Legendary Creature — Phyrexian Elder Dinosaur"
    power = 11
    toughness = 11
    oracleText = "Trample, indestructible\n" +
        "Whenever Etali deals combat damage to a player, they get that many poison counters. " +
        "(A player with ten or more poison counters loses the game.)"

    keywords(Keyword.TRAMPLE, Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.POISON,
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Ryan Pancoast"
        flavorText = "The contagion spreads and the Multiverse quakes."
        imageUri = "https://cards.scryfall.io/normal/back/9/5/95c14c4d-6c16-4826-8d93-d89ad04aee09.jpg?1783916997"
    }
}

val EtaliPrimalConqueror: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = EtaliPrimalConquerorFront,
    backFace = EtaliPrimalSickness,
)
