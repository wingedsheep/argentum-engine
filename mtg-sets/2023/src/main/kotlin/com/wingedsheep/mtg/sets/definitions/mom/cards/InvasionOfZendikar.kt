package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Invasion of Zendikar // Awakened Skyclave — March of the Machine #194.
 * {3}{G} · Battle — Siege · defense 3 // Creature — Elemental 4/4
 *
 * Front: ETB ramp of up to two basic lands onto the battlefield tapped.
 * Back: vigilance, haste; a source-scoped Layer 4 `GrantCardType("LAND")` static (a static only
 * functions on the battlefield, which is exactly "as long as it's on the battlefield"), and a
 * {T}: any-color mana ability.
 */
private val InvasionOfZendikarFront = card("Invasion of Zendikar") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Battle — Siege"
    startingDefense = 3
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, search your library for up to two basic land cards, put them onto " +
        "the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fed056f-a8f5-41ec-a7d2-a80a238872d1.jpg?1783916973"
    }
}

private val AwakenedSkyclave = card("Awakened Skyclave") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "Vigilance, haste\n" +
        "As long as this creature is on the battlefield, it's a land in addition to its other types.\n" +
        "{T}: Add one mana of any color."

    keywords(Keyword.VIGILANCE, Keyword.HASTE)

    staticAbility {
        ability = GrantCardType("LAND", GroupFilter.source())
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Diego Gisbert"
        flavorText = "Nahiri tried to bend Zendikar to Phyrexia's will. In response, the world rose against her."
        imageUri = "https://cards.scryfall.io/normal/back/8/f/8fed056f-a8f5-41ec-a7d2-a80a238872d1.jpg?1783916973"
        ruling("2023-04-14", "Awakened Skyclave is cast as a creature spell. It can be countered and otherwise responded to. It won't be a land in addition to its other types until it enters the battlefield.")
        ruling("2023-04-14", "If Awakened Skyclave enters the battlefield, any ability that triggers whenever a land enters the battlefield will trigger.")
    }
}

val InvasionOfZendikar: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfZendikarFront,
    backFace = AwakenedSkyclave,
)
