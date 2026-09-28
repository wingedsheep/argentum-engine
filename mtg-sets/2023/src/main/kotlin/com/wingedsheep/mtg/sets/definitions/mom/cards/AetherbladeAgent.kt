package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aetherblade Agent // Gitaxian Mindstinger (March of the Machine #88)
 * {1}{B} Creature — Human Rogue 1/1 // Creature — Phyrexian Rogue 3/3 (blue-black color indicator)
 *
 * Front — Deathtouch. "{4}{U/P}: Transform this creature. Activate only as a sorcery."
 * Back  — Deathtouch. Whenever this creature deals combat damage to a player or battle, draw a card.
 */
private val AetherbladeAgentFront = card("Aetherblade Agent") {
    manaCost = "{1}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\n" +
        "{4}{U/P}: Transform this creature. Activate only as a sorcery. " +
        "({U/P} can be paid with either {U} or 2 life.)"

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Mana("{4}{U/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Alexander Mokhov"
        flavorText = "The Consulate of Kaladesh had always valued his particular talent for extracting information . . ."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dad34ae5-56b4-4394-be02-e043dc1cc23d.jpg?1783917023"
    }
}

private val GitaxianMindstinger = card("Gitaxian Mindstinger") {
    manaCost = ""
    colorIndicator = "UB" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "UB"
    typeLine = "Creature — Phyrexian Rogue"
    power = 3
    toughness = 3
    oracleText = "Deathtouch\n" +
        "Whenever this creature deals combat damage to a player or battle, draw a card."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Alexander Mokhov"
        flavorText = ". . . but the Chrome Host's methods were far more efficient."
        imageUri = "https://cards.scryfall.io/normal/back/d/a/dad34ae5-56b4-4394-be02-e043dc1cc23d.jpg?1783917023"
    }
}

val AetherbladeAgent: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AetherbladeAgentFront,
    backFace = GitaxianMindstinger,
)
