package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity

/**
 * Scrapwork Cohort — The Brothers' War #37
 * {4} · Artifact Creature — Soldier · Common
 * 3/1
 *
 * When this creature enters, create a 1/1 colorless Soldier artifact creature token.
 * Unearth {2}{W}
 */
val ScrapworkCohort = card("Scrapwork Cohort") {
    manaCost = "{4}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Soldier"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, create a 1/1 colorless Soldier artifact creature token.\n" +
        "Unearth {2}{W} ({2}{W}: Return this card from your graveyard to the battlefield. It gains haste. Exile it at " +
        "the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            artifactToken = true
        )
    }

    unearth("{2}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91b0f17e-603a-4840-b2c3-381ec6f102f3.jpg?1783920119"
        ruling("2022-10-14", "If you activate a card's unearth ability but that card is removed from your graveyard before the ability resolves, that unearth ability will do nothing as it resolves.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it!")
    }
}
