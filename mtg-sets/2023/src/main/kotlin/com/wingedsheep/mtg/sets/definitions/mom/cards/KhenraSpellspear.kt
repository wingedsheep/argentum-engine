package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Khenra Spellspear // Gitaxian Spellstalker (March of the Machine #151)
 * {1}{R} Creature — Jackal Warrior 2/2 // Creature — Phyrexian Jackal 3/3 (red-blue color indicator)
 *
 * Front — Trample, prowess. "{3}{U/P}: Transform this creature. Activate only as a sorcery."
 * Back  — Trample, ward {2}, prowess, prowess (each instance triggers separately).
 */
private val KhenraSpellspearFront = card("Khenra Spellspear") {
    manaCost = "{1}{R}"
    colorIdentity = "RU"
    typeLine = "Creature — Jackal Warrior"
    power = 2
    toughness = 2
    oracleText = "Trample\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "{3}{U/P}: Transform this creature. Activate only as a sorcery. " +
        "({U/P} can be paid with either {U} or 2 life.)"

    keywords(Keyword.TRAMPLE)
    prowess()

    activatedAbility {
        cost = Costs.Mana("{3}{U/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc1454ce-bb49-4b4f-bbca-d77387fa4966.jpg?1783916992"
    }
}

private val GitaxianSpellstalker = card("Gitaxian Spellstalker") {
    manaCost = ""
    colorIndicator = "UR" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "RU"
    typeLine = "Creature — Phyrexian Jackal"
    power = 3
    toughness = 3
    oracleText = "Trample, ward {2}, prowess, prowess (Each instance of prowess triggers separately.)"

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    prowess()
    prowess()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Artur Nakhodkin"
        flavorText = "\"Bolas's methods were crude and inefficient, taking generations to refine the population " +
            "into a reliable source of combat units. Still, it offers us something of a head start.\"\n" +
            "—Jin-Gitaxias"
        imageUri = "https://cards.scryfall.io/normal/back/d/c/dc1454ce-bb49-4b4f-bbca-d77387fa4966.jpg?1783916992"
    }
}

val KhenraSpellspear: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = KhenraSpellspearFront,
    backFace = GitaxianSpellstalker,
)
