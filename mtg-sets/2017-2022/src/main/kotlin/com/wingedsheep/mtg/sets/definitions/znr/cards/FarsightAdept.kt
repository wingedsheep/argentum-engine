package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Farsight Adept — Zendikar Rising #14
 * {2}{W} · Creature — Kor Wizard · 3/3
 *
 * When this creature enters, you and target opponent each draw a card.
 */
val FarsightAdept = card("Farsight Adept") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Wizard"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you and target opponent each draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.DrawCards(1) then Effects.DrawCards(1, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Cristi Balanescu"
        flavorText = "\"I've walked every one of the Teetering Peaks and never set a foot wrong. I can get you to safety.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5949bb9a-b4e8-4992-a12d-8e31953aff0d.jpg?1783929418"
    }
}
