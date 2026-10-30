package com.wingedsheep.mtg.sets.definitions.dmc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Historian's Boon
 * {3}{W}
 * Enchantment
 *
 * Whenever this enchantment or another nontoken enchantment you control enters, create a 1/1
 * white Soldier creature token.
 * Whenever the final chapter ability of a Saga you control triggers, create a 4/4 white Angel
 * creature token with flying and vigilance.
 *
 * "This enchantment or another nontoken enchantment you control" is one unbound watcher over
 * nontoken enchantments you control (the Boon is one itself), as on Eidolon of Blossoms. The Angel
 * trigger fires as the final chapter ability (CR 714.2e) goes on the stack — even if that ability
 * is later countered — so it resolves before the chapter does.
 */
val HistoriansBoon = card("Historian's Boon") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever this enchantment or another nontoken enchantment you control enters, create a 1/1 white " +
        "Soldier creature token.\n" +
        "Whenever the final chapter ability of a Saga you control triggers, create a 4/4 white Angel creature " +
        "token with flying and vigilance."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.nontoken().youControl()).enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
        )
        description = "Whenever this enchantment or another nontoken enchantment you control enters, " +
            "create a 1/1 white Soldier creature token."
    }

    triggeredAbility {
        trigger = Triggers.you.finalSagaChapterTriggers()
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
        )
        description = "Whenever the final chapter ability of a Saga you control triggers, create a 4/4 white " +
            "Angel creature token with flying and vigilance."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0da48d1e-0dec-461a-9a1d-477408b297e6.jpg?1783921470"
        ruling(
            "2022-09-09",
            "The final chapter ability of a Saga is the ability with the greatest chapter number among " +
                "chapter abilities that Saga has."
        )
    }
}
