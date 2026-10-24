package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Professor of Zoomancy — Strixhaven: School of Mages #140 (canonical printing)
 * {3}{G} · Creature — Bear Druid · 4/3
 *
 * When this creature enters, create a 1/1 black and green Pest creature token with "When this
 * token dies, you gain 1 life."
 *
 * The Pest is the predefined Witherbloom token ([Effects.CreatePest]).
 */
val ProfessorOfZoomancy = card("Professor of Zoomancy") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear Druid"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, create a 1/1 black and green Pest creature token with " +
        "\"When this token dies, you gain 1 life.\""

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePest()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Rudy Siswanto"
        flavorText = "She's brilliant, if a bit overbearing."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8347f2e8-ef70-4879-860a-12f7cab25902.jpg?1783927340"
    }
}
