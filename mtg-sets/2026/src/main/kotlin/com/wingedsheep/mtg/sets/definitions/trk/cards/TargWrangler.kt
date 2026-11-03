package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Targ Wrangler
 * {2}{R}
 * Creature — Klingon Warrior
 * 5/3
 *
 * Menace
 * At the beginning of your end step, if this creature attacked this turn, sacrifice a nonland
 * permanent.
 *
 * The intervening-if end-step shape of Riders of the Mark; the sacrifice is Dreamshaper Shaman's
 * "sacrifice a nonland permanent" (the controller chooses, and the Wrangler itself is eligible).
 */
val TargWrangler = card("Targ Wrangler") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Klingon Warrior"
    power = 5
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "At the beginning of your end step, if this creature attacked this turn, sacrifice a nonland permanent."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.SourceAttackedThisTurn
        effect = Effects.SacrificeOwn(filter = GameObjectFilter.NonlandPermanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Harkalé Linaï"
        flavorText = "\"You complain about the conditions, yet you fail to appreciate that I keep them on leashes.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eabc4252-cf58-48c3-9483-7862450e03d0.jpg?1793100726"
    }
}
