package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The First Iroan Games
 * {2}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Create a 1/1 white Human Soldier creature token.
 * II — Put three +1/+1 counters on target creature you control.
 * III — If you control a creature with power 4 or greater, draw two cards.
 * IV — Create a Gold token.
 *
 * Chapter III's "if" is checked as the ability resolves (it isn't an intervening-if trigger
 * condition), reading projected power.
 */
private const val HUMAN_SOLDIER_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/1/e/1e76f0e3-9411-401d-ab38-9c3c64769483.jpg?1783931455"

val TheFirstIroanGames = card("The First Iroan Games") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Create a 1/1 white Human Soldier creature token.\n" +
        "II — Put three +1/+1 counters on target creature you control.\n" +
        "III — If you control a creature with power 4 or greater, draw two cards.\n" +
        "IV — Create a Gold token. (It's an artifact with \"Sacrifice this token: Add one mana of any color.\")"

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = HUMAN_SOLDIER_TOKEN_IMAGE,
        )
    }

    sagaChapter(2) {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, creature)
    }

    sagaChapter(3) {
        effect = Effects.If(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            then = Effects.DrawCards(2),
        )
    }

    sagaChapter(4) {
        effect = Effects.CreateGold()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b05ada5-8e5f-4158-bd28-e6c24e4a2299.jpg?1783931539"
        ruling(
            "2020-01-24",
            "A Gold token is a colorless Gold artifact token with \"Sacrifice this artifact: Add one mana of any color.\"",
        )
    }
}
