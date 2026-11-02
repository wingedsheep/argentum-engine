package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Patient Rebuilding
 * {3}{U}{U}
 * Enchantment
 * At the beginning of your upkeep, target opponent mills three cards, then you draw a card for
 * each land card put into their graveyard this way.
 *
 * The milled collection is filtered to land cards that are *currently in a graveyard*, so a card a
 * replacement effect sent elsewhere (Rest in Peace) isn't counted — the 2018-07-13 ruling. Only the
 * cards this resolution milled are counted, never lands already in their graveyard.
 */
val PatientRebuilding = card("Patient Rebuilding") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, target opponent mills three cards, then you draw a card " +
        "for each land card put into their graveyard this way."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val milled = mill(3, Player.TargetOpponent)
            val lands = filter(milled, GameObjectFilter.Land.currentlyIn(Zone.GRAVEYARD))
            run(Effects.DrawCards(lands.count))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Magali Villeneuve"
        flavorText = "Nicol Bolas would not rest until he was restored to his former glory."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2256e07f-a35a-4393-9744-045564d5770b.jpg?1783934584"
        ruling("2018-07-13", "Each time Patient Rebuilding's triggered ability resolves, it counts only the land cards put into that graveyard during that ability's resolution. For example, if your opponent puts two land cards into their graveyard during one upkeep and three land cards the next, you'll draw two and three cards, respectively, not two and five.")
        ruling("2018-07-13", "If your opponent moves cards from their library but they aren't put into their graveyard (perhaps because you control Rest in Peace), you won't draw any cards.")
    }
}
