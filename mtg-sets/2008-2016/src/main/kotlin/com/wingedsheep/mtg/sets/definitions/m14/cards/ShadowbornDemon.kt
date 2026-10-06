package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shadowborn Demon
 * {3}{B}{B}
 * Creature — Demon
 * 5/6
 *
 * Flying
 * When this creature enters, destroy target non-Demon creature.
 * At the beginning of your upkeep, if there are fewer than six creature cards in your graveyard,
 * sacrifice a creature.
 *
 * Canonical printing: Magic 2014, the card's earliest real printing.
 *
 * The upkeep ability is an intervening "if" (CR 603.4): `interveningIf` checks the graveyard both
 * when the trigger would fire and again on resolution, which is exactly the second ruling.
 */
val ShadowbornDemon = card("Shadowborn Demon") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText = "Flying\nWhen this creature enters, destroy target non-Demon creature.\nAt the beginning of your upkeep, if there are fewer than six creature cards in your graveyard, sacrifice a creature."
    power = 5
    toughness = 6

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.notSubtype(Subtype("Demon")))
        effect = Effects.Destroy(t)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.Not(Conditions.CreatureCardsInGraveyardAtLeast(6))
        effect = Effects.SacrificeOwn(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3884c05b-c10e-4f1d-a8bd-8b5118657972.jpg?1783939920"
        ruling("2013-07-01", "Shadowborn Demon's enters-the-battlefield ability is mandatory. If you control the only non-Demon creature, you must choose it as the target.")
        ruling("2013-07-01", "The last ability checks whether you have fewer than six creature cards in your graveyard when it would trigger. If you have six or more, it won't trigger at all. The ability will check again when it tries to resolve. If at that time you have six or more, the ability won't do anything.")
    }
}
