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
 * Harried Artisan // Phyrexian Skyflayer (March of the Machine #143)
 * {2}{R} Creature — Human Artificer 2/3 // Creature — Phyrexian Artificer 3/4 (red-white color indicator)
 *
 * Front — Haste. "{3}{W/P}: Transform this creature. Activate only as a sorcery."
 * Back  — Flying, haste.
 *
 * The {W/P} pip is paid with {W} or 2 life (CR 107.4f) — the usual way a mono-red deck flips it.
 */
private val HarriedArtisanFront = card("Harried Artisan") {
    manaCost = "{2}{R}"
    colorIdentity = "WR"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 3
    oracleText = "Haste\n" +
        "{3}{W/P}: Transform this creature. Activate only as a sorcery. " +
        "({W/P} can be paid with either {W} or 2 life.)"

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{3}{W/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Caio Monteiro"
        flavorText = "As strange portals roared open across Theros, Lydus gathered his prototype and " +
            "fled. He was so close to perfecting his design."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73e92389-4bd2-492e-b4d6-d7cb6baedc41.jpg?1783916997"
    }
}

private val PhyrexianSkyflayer = card("Phyrexian Skyflayer") {
    manaCost = ""
    colorIndicator = "WR" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "WR"
    typeLine = "Creature — Phyrexian Artificer"
    power = 3
    toughness = 4
    oracleText = "Flying, haste"

    keywords(Keyword.FLYING, Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Caio Monteiro"
        flavorText = "Soaring in Realmbreaker's embrace, he finally understood what perfection was."
        imageUri = "https://cards.scryfall.io/normal/back/7/3/73e92389-4bd2-492e-b4d6-d7cb6baedc41.jpg?1783916997"
    }
}

val HarriedArtisan: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HarriedArtisanFront,
    backFace = PhyrexianSkyflayer,
)
