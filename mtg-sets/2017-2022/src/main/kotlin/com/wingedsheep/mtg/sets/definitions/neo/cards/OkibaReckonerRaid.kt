package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Okiba Reckoner Raid // Nezumi Road Captain
 * {B} — Enchantment — Saga // Enchantment Creature — Rat Rogue 2/2
 *
 * Front — Okiba Reckoner Raid:
 *   I, II — Each opponent loses 1 life and you gain 1 life.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Nezumi Road Captain:
 *   Menace
 *   Vehicles you control have menace.
 *
 * Chapters I and II are two separate life effects rather than a drain: the gain is a flat 1, not
 * "life equal to the life lost", so in multiplayer every opponent loses 1 but you still gain only 1.
 * The Vehicle grant uses [GameObjectFilter.Any] so an uncrewed (noncreature) Vehicle also has menace.
 */
private val NezumiRoadCaptain = card("Nezumi Road Captain") {
    manaCost = ""
    colorIndicator = "B"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Rat Rogue"
    oracleText = "Menace\nVehicles you control have menace. (They can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 2

    keywords(Keyword.MENACE)

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.MENACE,
            filter = GroupFilter(GameObjectFilter.Any.withAnySubtype("Vehicle").youControl()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Victor Adame Minguez"
        flavorText = "United by defiant pride in their outsider status, the nezumi of the Okiba Reckoners " +
            "boast the most advanced cyberbikes and the best riders of all the Reckoner gangs."
        imageUri = "https://cards.scryfall.io/normal/back/4/f/4f0582b4-d951-4450-b158-4a34109e48cd.jpg?1783923885"
    }
}

private val OkibaReckonerRaidFront = card("Okiba Reckoner Raid") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Each opponent loses 1 life and you gain 1 life.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    sagaChapter(2) {
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f0582b4-d951-4450-b158-4a34109e48cd.jpg?1783923885"
    }
}

val OkibaReckonerRaid: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = OkibaReckonerRaidFront,
    backFace = NezumiRoadCaptain,
)
