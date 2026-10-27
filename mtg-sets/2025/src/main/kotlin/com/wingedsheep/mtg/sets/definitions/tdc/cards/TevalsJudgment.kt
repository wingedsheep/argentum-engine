package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode

/**
 * Teval's Judgment — Tarkir: Dragonstorm Commander #28 (canonical printing)
 * {2}{B} · Enchantment
 *
 * Whenever one or more cards leave your graveyard, choose one that hasn't been chosen this turn —
 * • Draw a card.
 * • Create a Treasure token.
 * • Create a 2/2 black Zombie Druid creature token.
 *
 * "One or more … leave" is CR 603.2c batch wording — `Triggers.oneOrMore(...).leaveYourGraveyard()`
 * fires once per batch, as on Stonebound Mentor. The mode memory is
 * [ModalEffect.chooseOneNotYetChosenThisTurn] (Breeches, Eager Pillager): each mode is chosen at
 * most once per turn for this enchantment, and once all three have been chosen further triggers
 * that turn resolve with no effect.
 */
val TevalsJudgment = card("Teval's Judgment") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more cards leave your graveyard, choose one that hasn't been chosen this turn —\n" +
        "• Draw a card.\n" +
        "• Create a Treasure token.\n" +
        "• Create a 2/2 black Zombie Druid creature token."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).leaveYourGraveyard()
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            Mode.noTarget(Effects.DrawCards(1), "Draw a card"),
            Mode.noTarget(Effects.CreateTreasure(1), "Create a Treasure token"),
            Mode.noTarget(
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie", "Druid"),
                    name = "Zombie Druid",
                    imageUri = "https://cards.scryfall.io/normal/front/f/1/f10d5813-7818-43e8-b08d-4ed8c54d0366.jpg?1783906785",
                ),
                "Create a 2/2 black Zombie Druid creature token",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40ed0cef-8ff6-4169-a9f3-7dd5cf56b79f.jpg?1783907175"
    }
}
