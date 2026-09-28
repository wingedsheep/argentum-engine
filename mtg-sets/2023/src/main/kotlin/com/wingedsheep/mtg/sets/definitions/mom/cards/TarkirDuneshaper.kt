package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tarkir Duneshaper // Burnished Dunestomper (March of the Machine #43)
 * {W} Creature — Dog Warrior 1/2 // Creature — Phyrexian Dog Warrior 4/3 (green-white color indicator)
 *
 * Front — "{4}{G/P}: Transform this creature. Activate only as a sorcery."
 * Back  — Trample.
 *
 * The {G/P} pip is paid with {G} or 2 life (CR 107.4f).
 */
private val TarkirDuneshaperFront = card("Tarkir Duneshaper") {
    manaCost = "{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Dog Warrior"
    power = 1
    toughness = 2
    oracleText = "{4}{G/P}: Transform this creature. Activate only as a sorcery. " +
        "({G/P} can be paid with either {G} or 2 life.)"

    activatedAbility {
        cost = Costs.Mana("{4}{G/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Denys Tsiperko"
        flavorText = "\"Obey the teachings of the dragonlords, for they provide all. They grant us magic " +
            "and safety, and in return we offer respect.\"\n—Baihir, Dromoka mage"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdc37acc-05ba-4457-8a03-d635497bfb1b.jpg?1783917051"
    }
}

private val BurnishedDunestomper = card("Burnished Dunestomper") {
    manaCost = ""
    colorIndicator = "GW" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "GW"
    typeLine = "Creature — Phyrexian Dog Warrior"
    power = 4
    toughness = 3
    oracleText = "Trample"

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Denys Tsiperko"
        flavorText = "\"Your precious dragonlords repressed your instincts, cur. You were born a hunter. " +
            "Evolve, and hunt again.\"\n—Benzir, archdruid of Temple Might"
        imageUri = "https://cards.scryfall.io/normal/back/f/d/fdc37acc-05ba-4457-8a03-d635497bfb1b.jpg?1783917051"
    }
}

val TarkirDuneshaper: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = TarkirDuneshaperFront,
    backFace = BurnishedDunestomper,
)
