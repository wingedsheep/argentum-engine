package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mazirek, Kraul Death Priest
 * {3}{B}{G}
 * Legendary Creature — Insect Shaman
 * 2/2
 *
 * Flying
 * Whenever a player sacrifices another permanent, put a +1/+1 counter on each creature you control.
 *
 * "A player" is the [Triggers.anyPlayer] scope; "another" is the OTHER-binding per-permanent
 * sacrifice trigger, so it fires once for each other permanent any player sacrifices (CR 603.2c),
 * and — per the ruling — still fires for the others when Mazirek is sacrificed alongside them.
 */
val MazirekKraulDeathPriest = card("Mazirek, Kraul Death Priest") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Insect Shaman"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever a player sacrifices another permanent, put a +1/+1 counter on each creature you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.sacrificesAnother(GameObjectFilter.Permanent)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "48"
        artist = "Mathias Kollros"
        flavorText = "The clicks and buzz of the undercity proclaim him the incarnation of decay."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c3ac233-29f7-4ed7-9363-85022d72f633.jpg?1783938107"

        ruling(
            "2020-08-07",
            "A legendary permanent that is put into a graveyard because of the \"legend rule\" isn't sacrificed."
        )
        ruling(
            "2020-08-07",
            "If you sacrifice Mazirek and other permanents at the same time, Mazirek's last ability triggers for the other permanents."
        )
        ruling(
            "2020-08-07",
            "If you sacrifice a permanent as part of casting a spell or activating an ability, Mazirek's last ability will resolve before that spell or ability."
        )
    }
}
