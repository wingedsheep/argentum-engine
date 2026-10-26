package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mikaeus, the Unhallowed
 * {3}{B}{B}{B}
 * Legendary Creature — Zombie Cleric
 * 5/5
 *
 * Intimidate
 * Whenever a Human deals damage to you, destroy it.
 * Other non-Human creatures you control get +1/+1 and have undying.
 *
 * Canonical printing: Dark Ascension, the card's earliest real printing. The damage trigger binds
 * the damage source as the triggering entity, so "destroy it" destroys that Human — including one
 * you control (ruling 2018-12-07). The granted undying is read off the dying creature's last-known
 * keywords, so a creature dying alongside Mikaeus still returns.
 */
private val otherNonHumansYouControl = GroupFilter(
    GameObjectFilter.Creature.notSubtype(Subtype("Human")).youControl(),
    excludeSelf = true,
)

val MikaeusTheUnhallowed = card("Mikaeus, the Unhallowed") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Zombie Cleric"
    power = 5
    toughness = 5
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that " +
        "share a color with it.)\n" +
        "Whenever a Human deals damage to you, destroy it.\n" +
        "Other non-Human creatures you control get +1/+1 and have undying. (When a creature with undying dies, " +
        "if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 " +
        "counter on it.)"

    keywords(Keyword.INTIMIDATE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.withSubtype(Subtype("Human"))).dealsDamage(Recipient.You)
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = otherNonHumansYouControl,
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.UNDYING, otherNonHumansYouControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "70"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/801b124d-3546-4882-a6e1-c9c353628a18.jpg?1783940827"
        ruling(
            "2018-12-07",
            "Any damage dealt to you by a Human will cause Mikaeus's ability to trigger, including noncombat damage " +
                "dealt to you by a Human and damage dealt to you by a Human you control."
        )
        ruling(
            "2018-12-07",
            "If a Human deals damage to you at the same time that Mikaeus is dealt lethal damage, Mikaeus's " +
                "triggered ability triggers. That Human will be destroyed even though Mikaeus has left the battlefield."
        )
        ruling(
            "2018-12-07",
            "If a non-Human creature you control without a +1/+1 counter dies at the same time as Mikaeus, that " +
                "creature's undying ability granted by Mikaeus triggers and will return it to the battlefield."
        )
        ruling(
            "2018-12-07",
            "The +1/+1 bonus that Mikaeus gives to other non-Human creatures you control isn't a counter. It won't " +
                "prevent undying from triggering."
        )
    }
}
