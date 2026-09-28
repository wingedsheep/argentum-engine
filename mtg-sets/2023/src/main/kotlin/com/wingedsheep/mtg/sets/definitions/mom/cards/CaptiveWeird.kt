package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Captive Weird // Compleated Conjurer (March of the Machine #49)
 * {U} Creature — Weird 1/3 // Creature — Phyrexian Weird 3/3 (red-blue color indicator)
 *
 * Front — "Defender. {3}{R/P}: Transform this creature. Activate only as a sorcery."
 * Back  — "When this creature transforms into Compleated Conjurer, exile the top card of your library.
 *          Until the end of your next turn, you may play that card."
 */
private val CaptiveWeirdFront = card("Captive Weird") {
    manaCost = "{U}"
    colorIdentity = "UR"
    typeLine = "Creature — Weird"
    power = 1
    toughness = 3
    oracleText = "Defender\n{3}{R/P}: Transform this creature. Activate only as a sorcery. " +
        "({R/P} can be paid with either {R} or 2 life.)"

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{3}{R/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Manuel Castañón"
        flavorText = "Izzet researchers agreed to keep the unusual weird contained until they " +
            "understood the extent of its powers."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70668650-0fb1-4486-a4e6-ab9a12be5626.jpg?1783917056"
    }
}

private val CompleatedConjurer = card("Compleated Conjurer") {
    manaCost = ""
    colorIndicator = "UR" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "UR"
    typeLine = "Creature — Phyrexian Weird"
    power = 3
    toughness = 3
    oracleText = "When this creature transforms into Compleated Conjurer, exile the top card of your " +
        "library. Until the end of your next turn, you may play that card."

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        effect = Patterns.Exile.impulse(1, MayPlayExpiry.UntilEndOfNextTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Manuel Castañón"
        flavorText = "The Furnace Host saw no need for such caution."
        imageUri = "https://cards.scryfall.io/normal/back/7/0/70668650-0fb1-4486-a4e6-ab9a12be5626.jpg?1783917056"
    }
}

val CaptiveWeird: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = CaptiveWeirdFront,
    backFace = CompleatedConjurer,
)
