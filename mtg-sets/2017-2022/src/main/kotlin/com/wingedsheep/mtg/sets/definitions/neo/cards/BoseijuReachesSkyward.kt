package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Boseiju Reaches Skyward // Branch of Boseiju
 * {3}{G} — Enchantment — Saga // Enchantment Creature — Plant 0/0
 *
 * Front — Boseiju Reaches Skyward:
 *   I — Search your library for up to two basic Forest cards, reveal them, put them into your hand,
 *       then shuffle.
 *   II — Put up to one target land card from your graveyard on top of your library.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Branch of Boseiju:
 *   Reach
 *   This creature gets +1/+1 for each land you control.
 */
private val BranchOfBoseiju = card("Branch of Boseiju") {
    manaCost = ""
    colorIndicator = "G"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Plant"
    oracleText = "Reach\nThis creature gets +1/+1 for each land you control."
    power = 0
    toughness = 0

    keywords(Keyword.REACH)

    dynamicStats(DynamicAmounts.landsYouControl())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Zezhou Chen"
        flavorText = "Though they razed the surrounding forest, the builders of Towashi left Boseiju " +
            "unscathed, shaping the city around the ancient tree."
        imageUri = "https://cards.scryfall.io/normal/back/1/1/1144014b-f13b-4397-97ed-a8de46371a2c.jpg?1783923858"
    }
}

private val BoseijuReachesSkywardFront = card("Boseiju Reaches Skyward") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Search your library for up to two basic Forest cards, reveal them, put them into your " +
        "hand, then shuffle.\n" +
        "II — Put up to one target land card from your graveyard on top of your library.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.FOREST),
            count = 2,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    sagaChapter(2) {
        val land = target(
            TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true
        )
        effect = Effects.PutOnTopOfLibrary(land)
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1144014b-f13b-4397-97ed-a8de46371a2c.jpg?1783923858"
    }
}

val BoseijuReachesSkyward: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = BoseijuReachesSkywardFront,
    backFace = BranchOfBoseiju,
)
