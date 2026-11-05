package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Apocalypse Demon — Hour of Devastation #58 (canonical printing)
 * {4}{B}{B} · Creature — Demon · * / *
 *
 * Flying
 * Apocalypse Demon's power and toughness are each equal to the number of cards in your graveyard.
 * At the beginning of your upkeep, tap this creature unless you sacrifice another creature.
 *
 * The P/T is a characteristic-defining ability (`dynamicStats`), so it applies in every zone. The
 * upkeep tax is a resolution-time punisher: [Costs.pay.SacrificeAnother] keeps the Demon itself out
 * of the sacrifice pool, so with no other creature the Demon simply taps.
 */
val ApocalypseDemon = card("Apocalypse Demon") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText = "Flying\n" +
        "Apocalypse Demon's power and toughness are each equal to the number of cards in your graveyard.\n" +
        "At the beginning of your upkeep, tap this creature unless you sacrifice another creature."

    dynamicStats(DynamicAmounts.cardsInYourGraveyard())

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.SacrificeAnother(GameObjectFilter.Creature),
            suffer = Effects.Tap(EffectTarget.Self),
        )
        description = "At the beginning of your upkeep, tap this creature unless you sacrifice another creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Piotr Jabłoński"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39dfbf99-55e8-468b-bcd9-9179cad7cab6.jpg?1783936042"
        ruling("2017-07-14", "The ability that defines Apocalypse Demon's power and toughness works in all zones, not just the battlefield.")
    }
}
