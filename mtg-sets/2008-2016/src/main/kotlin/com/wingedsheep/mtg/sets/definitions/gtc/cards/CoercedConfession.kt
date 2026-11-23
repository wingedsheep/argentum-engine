package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Coerced Confession — Gatecrash #217
 * {4}{U/B} · Sorcery
 *
 * Target player mills four cards. You draw a card for each creature card put into their graveyard
 * this way.
 *
 * Patient Rebuilding's shape on a targeted player: the pipeline mill returns only the cards this
 * resolution milled, filtered to creature cards *currently in a graveyard* — so a card a replacement
 * effect sent elsewhere (Rest in Peace) isn't counted, and creature cards already in that graveyard
 * never are.
 */
val CoercedConfession = card("Coerced Confession") {
    manaCost = "{4}{U/B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target player mills four cards. You draw a card for each creature card put into their graveyard this way."

    spell {
        target(Targets.Player)
        effect = Effects.Pipeline {
            val milled = mill(4, Player.TargetPlayer)
            val creatures = filter(milled, GameObjectFilter.Creature.currentlyIn(Zone.GRAVEYARD))
            run(Effects.DrawCards(creatures.count))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Mathias Kollros"
        flavorText = "\"Ask the right questions in the right way and truth is inevitable.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d76102f1-b05f-472b-81cf-eae424c55638.jpg?1783940095"
    }
}
