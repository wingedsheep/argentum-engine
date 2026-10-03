package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Drowner of Truth {5}{G/U}{G/U} // Drowned Jungle — Modern Horizons 3 #253 (uncommon)
 * Creature — Eldrazi 7/6
 * Devoid
 * When you cast this spell, if {C} was spent to cast it, create two 0/1 colorless Eldrazi Spawn
 * creature tokens with "Sacrifice this token: Add {C}."
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {G} or {U}.
 *
 * The cast trigger mirrors Wumpus Aberration's: an intervening "if" over the payment recorded on
 * the spell, so it resolves (and makes its Spawn) even if the spell is countered in response.
 */
private val DrownerOfTruthFront = card("Drowner of Truth") {
    manaCost = "{5}{G/U}{G/U}"
    colorIdentity = "GU"
    typeLine = "Creature — Eldrazi"
    power = 7
    toughness = 6
    oracleText = "Devoid (This card has no color.)\nWhen you cast this spell, if {C} was spent to cast it, " +
        "create two 0/1 colorless Eldrazi Spawn creature tokens with \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.ManaSpentToCastIncludes(requiredColorless = 1)
        effect = Effects.CreateEldraziSpawn(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Nicholas Gregory"
        flavorText = "\"Mangeni's children take their places among us.\"\n—Ayli, high priest of the Eternal Pilgrims"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a1d3c1d-1373-4ac4-bb26-9780976efc4f.jpg?1783911227"
        ruling("2024-06-07", "Drowner of Truth's triggered ability will resolve before Drowner of Truth does. If Drowner of Truth is countered or otherwise leaves the stack in response to that triggered ability, the triggered ability will still resolve as normal.")
    }
}

private val DrownedJungleBack = card("Drowned Jungle") {
    typeLine = "Land"
    colorIdentity = "GU"
    oracleText = "This land enters tapped.\n{T}: Add {G} or {U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Nicholas Gregory"
        flavorText = "\"Eldrazi scour the world of impurity so that we, the faithful, will inherit paradise.\"\n—Ayli, high priest of the Eternal Pilgrims"
        imageUri = "https://cards.scryfall.io/normal/back/7/a/7a1d3c1d-1373-4ac4-bb26-9780976efc4f.jpg?1783911227"
    }
}

val DrownerOfTruth: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = DrownerOfTruthFront,
    backFace = DrownedJungleBack,
)
