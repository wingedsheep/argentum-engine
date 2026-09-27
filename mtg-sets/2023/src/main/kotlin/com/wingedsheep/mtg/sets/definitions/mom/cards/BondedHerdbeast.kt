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
 * Bonded Herdbeast // Plated Kilnbeast (March of the Machine #178)
 * {4}{G} Creature — Beast 4/5 // Creature — Phyrexian Beast 7/5 (red-green color indicator)
 *
 * Front — "{4}{R/P}: Transform this creature. Activate only as a sorcery."
 * Back  — Menace.
 *
 * The {R/P} pip is paid with {R} or 2 life (CR 107.4f); auto-pay spends life only when no red
 * source can cover it.
 */
private val BondedHerdbeastFront = card("Bonded Herdbeast") {
    manaCost = "{4}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 5
    oracleText = "{4}{R/P}: Transform this creature. Activate only as a sorcery. " +
        "({R/P} can be paid with either {R} or 2 life.)"

    activatedAbility {
        cost = Costs.Mana("{4}{R/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Jokubas Uogintas"
        flavorText = "Though both had lost their families, the *eludha*—the spiritual connection " +
            "between the monsters of Ikoria and their human bonders—gave them something greater."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61972ddb-3421-4f22-a47a-89cea944dd02.jpg?1783916982"
    }
}

private val PlatedKilnbeast = card("Plated Kilnbeast") {
    manaCost = ""
    colorIndicator = "RG" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "RG"
    typeLine = "Creature — Phyrexian Beast"
    power = 7
    toughness = 5
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)"

    keywords(Keyword.MENACE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Jokubas Uogintas"
        flavorText = "Now, united by the Great Work, they knew what it meant to be truly bonded."
        imageUri = "https://cards.scryfall.io/normal/back/6/1/61972ddb-3421-4f22-a47a-89cea944dd02.jpg?1783916982"
    }
}

val BondedHerdbeast: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BondedHerdbeastFront,
    backFace = PlatedKilnbeast,
)
