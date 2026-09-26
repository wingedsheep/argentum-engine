package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Student of Elements // Tobita, Master of Winds (Champions of Kamigawa #93) — a flip card (CR 710).
 *
 * Student of Elements {1}{U} — Creature — Human Wizard 1/1
 * "When this creature has flying, flip it."
 *
 * Tobita, Master of Winds — Legendary Creature — Human Wizard 3/3
 * "Creatures you control have flying."
 *
 * "When this creature has flying" is a state trigger (CR 603.8): nothing *happens* when a
 * continuous effect grants flying, so the poller watches the projected keyword itself.
 */
private val StudentOfElementsUpright = card("Student of Elements") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature has flying, flip it."
    power = 1
    toughness = 1

    stateTriggeredAbility {
        condition = Conditions.SourceHasKeyword(Keyword.FLYING)
        effect = Effects.Flip()
        description = "When this creature has flying, flip it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Ittoku"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9de1eebf-5725-438c-bcf0-f3a4d8a89fb0.jpg?1783944320"
    }
}

private val TobitaMasterOfWinds = card("Tobita, Master of Winds") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    oracleText = "Creatures you control have flying."
    power = 3
    toughness = 3

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Ittoku"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9de1eebf-5725-438c-bcf0-f3a4d8a89fb0.jpg?1783944320"
    }
}

val StudentOfElements: CardDefinition = CardDefinition.flipCard(
    unflipped = StudentOfElementsUpright,
    flipped = TobitaMasterOfWinds,
)
