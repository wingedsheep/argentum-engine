package com.wingedsheep.mtg.sets.definitions.mic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hordewing Skaab — Innistrad: Midnight Hunt Commander #15 (canonical printing)
 * {4}{U} · Creature — Zombie Horror · 3/3
 *
 * Flying
 * Other Zombies you control have flying.
 * Whenever one or more Zombies you control deal combat damage to one or more of your opponents,
 * you may draw cards equal to the number of opponents dealt damage this way. If you do, discard
 * that many cards.
 *
 * One trigger per combat-damage step however many opponents were hit
 * (`dealCombatDamageToOneOrMoreOpponents`); the opponents it hit are the trigger's damaged-player
 * collection, so both the draw and the discard read that count — the discard stays "that many"
 * even if a replacement changed how many cards were drawn (ruling).
 */
val HordewingSkaab = card("Hordewing Skaab") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    power = 3
    toughness = 3
    oracleText = "Flying\nOther Zombies you control have flying.\n" +
        "Whenever one or more Zombies you control deal combat damage to one or more of your opponents, " +
        "you may draw cards equal to the number of opponents dealt damage this way. If you do, discard that many cards."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantKeyword(
            Keyword.FLYING,
            GroupFilter.AllCreaturesYouControl.withSubtype("Zombie").other()
        )
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withSubtype("Zombie"))
            .dealCombatDamageToOneOrMoreOpponents()
        effect = Effects.May(
            Effects.DrawCards(DynamicAmounts.playersDealtDamageThisWay()) then
                Patterns.Hand.discardCards(DynamicAmounts.playersDealtDamageThisWay())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e67802e-e0d9-4989-b1c2-77ef2ff69796.jpg?1783925379"
        ruling("2021-09-24", "The number of cards you discard is equal to the number of opponents that were dealt combat damage by your Zombies, even if another effect changed the number of cards you drew.")
        ruling("2021-09-24", "If some of your Zombies have first strike but others don't, or if you have a Zombie with double strike, the ability will trigger twice, the first time counting the number of opponents who were dealt damage in the first strike combat damage step, and the second time counting the ones who were dealt damage in the second combat damage step.")
    }
}
