package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Pontiff of Blight
 * {4}{B}{B}
 * Creature — Zombie Cleric
 * 2/7
 *
 * Extort
 * Other creatures you control have extort. (If a creature has multiple instances of extort, each
 * triggers separately.)
 *
 * Extort is composed as on Crypt Ghast: a cast trigger with a resolution-time [Effects.MayPay] of
 * `{W/B}` over [Effects.DrainLife]. The grant is the Bria, Riptide Rogue shape — a battlefield
 * [GrantTriggeredAbility] of that same trigger to other creatures you control. Each grant is its own
 * instance, so a creature with printed extort gets a second, separately triggering one.
 */
val PontiffOfBlight = card("Pontiff of Blight") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Cleric"
    power = 2
    toughness = 7
    oracleText = "$EXTORT_TEXT\n" +
        "Other creatures you control have extort. (If a creature has multiple instances of extort, " +
        "each triggers separately.)"

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.casts(),
                effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1)),
                descriptionOverride = EXTORT_TEXT,
            ),
            filter = GroupFilter.OtherCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72e5291f-9281-4cb7-9158-54b7cb336b93.jpg?1783940039"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
