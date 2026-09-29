package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Polukranos Reborn // Polukranos, Engine of Ruin (March of the Machine #200)
 * {G}{G}{G} Legendary Creature — Hydra 4/5 // Legendary Creature — Phyrexian Hydra 6/6 (green-white)
 *
 * Front — Reach. "{6}{W/P}: Transform Polukranos Reborn. Activate only as a sorcery."
 * Back  — Reach, lifelink. "Whenever Polukranos or another nontoken Hydra you control dies, create a
 * 3/3 green and white Phyrexian Hydra creature token with reach and a 3/3 green and white Phyrexian
 * Hydra creature token with lifelink."
 *
 * The dies trigger is split into a self half and an "another nontoken Hydra" half so Polukranos
 * itself counts even when it is a token (only the *other* Hydras must be nontoken).
 */
private val PolukranosRebornFront = card("Polukranos Reborn") {
    manaCost = "{G}{G}{G}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Hydra"
    power = 4
    toughness = 5
    oracleText = "Reach\n{6}{W/P}: Transform Polukranos Reborn. Activate only as a sorcery. " +
        "({W/P} can be paid with either {W} or 2 life.)"

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Mana("{6}{W/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Polukranos Reborn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "David Auden Nash"
        flavorText = "Freed from the Underworld, the World Eater resumed his endless feast with reckless enthusiasm."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47f7d313-8333-41fe-8bfa-c96774dac228.jpg?1783916971"
    }
}

private fun hydraTokens() = Effects.CreateToken(
    power = 3,
    toughness = 3,
    colors = setOf(Color.GREEN, Color.WHITE),
    creatureTypes = setOf("Phyrexian", "Hydra"),
    keywords = setOf(Keyword.REACH),
    imageUri = "https://cards.scryfall.io/normal/front/2/b/2bff78d9-7c4c-4b3b-a485-5328e985315b.jpg?1783916668"
) then Effects.CreateToken(
    power = 3,
    toughness = 3,
    colors = setOf(Color.GREEN, Color.WHITE),
    creatureTypes = setOf("Phyrexian", "Hydra"),
    keywords = setOf(Keyword.LIFELINK),
    imageUri = "https://cards.scryfall.io/normal/front/a/1/a1bb0ec0-729e-4dcb-bab4-9f31c1056ab3.jpg?1783916668"
)

private const val TOKEN_TEXT = "create a 3/3 green and white Phyrexian Hydra creature token with reach and " +
    "a 3/3 green and white Phyrexian Hydra creature token with lifelink."

private val PolukranosEngineOfRuin = card("Polukranos, Engine of Ruin") {
    manaCost = ""
    colorIndicator = "GW" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Phyrexian Hydra"
    power = 6
    toughness = 6
    oracleText = "Reach, lifelink\nWhenever Polukranos or another nontoken Hydra you control dies, " +
        "create a 3/3 green and white Phyrexian Hydra creature token with reach and a 3/3 green and " +
        "white Phyrexian Hydra creature token with lifelink."

    keywords(Keyword.REACH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = hydraTokens()
        description = "When Polukranos dies, $TOKEN_TEXT"
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withSubtype("Hydra").youControl().nontoken()).dies()
        effect = hydraTokens()
        description = "Whenever another nontoken Hydra you control dies, $TOKEN_TEXT"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "David Auden Nash"
        imageUri = "https://cards.scryfall.io/normal/back/4/7/47f7d313-8333-41fe-8bfa-c96774dac228.jpg?1783916971"
    }
}

val PolukranosReborn: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = PolukranosRebornFront,
    backFace = PolukranosEngineOfRuin,
)
