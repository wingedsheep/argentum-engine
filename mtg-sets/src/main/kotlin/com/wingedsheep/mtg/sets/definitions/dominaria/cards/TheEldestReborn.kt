package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Eldest Reborn
 * {4}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Each opponent sacrifices a creature or planeswalker.
 * II — Each opponent discards a card.
 * III — Put target creature or planeswalker card from a graveyard onto the battlefield
 *       under your control.
 */
val TheEldestReborn = card("The Eldest Reborn") {
    manaCost = "{4}{B}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Each opponent sacrifices a creature or planeswalker.\n" +
        "II — Each opponent discards a card.\n" +
        "III — Put target creature or planeswalker card from a graveyard onto the battlefield under your control."

    sagaChapter(1) {
        effect = Effects.Sacrifice(
            GameObjectFilter.CreatureOrPlaneswalker,
            target = EffectTarget.PlayerRef(Player.Opponent)
        )
    }

    sagaChapter(2) {
        effect = Effects.EachOpponentDiscards(1)
    }

    sagaChapter(3) {
        val graveyardTarget = target(
            "creature or planeswalker",
            TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = com.wingedsheep.sdk.core.Zone.GRAVEYARD))
        )
        effect = Effects.PutOntoBattlefieldUnderYourControl(graveyardTarget)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Ravenna Tran"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8318f40-ecd5-429e-8fe2-febf31f64841.jpg?1562742744"
    }
}
