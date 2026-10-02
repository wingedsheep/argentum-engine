package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Solphim, Mayhem Dominus
 * {2}{R}{R}
 * Legendary Creature — Phyrexian Horror
 * 5/4
 *
 * If a source you control would deal noncombat damage to an opponent or a permanent an opponent
 * controls, it deals double that damage to that player or permanent instead.
 * {1}{R/P}{R/P}, Discard two cards: Put an indestructible counter on Solphim.
 *
 * The doubling is a [DoubleDamage] replacement scoped to sources you control, noncombat damage,
 * and the "an opponent or a permanent an opponent controls" recipient — Twinflame Tyrant's shape
 * narrowed by `DamageType.NonCombat`.
 */
val SolphimMayhemDominus = card("Solphim, Mayhem Dominus") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Phyrexian Horror"
    power = 5
    toughness = 4
    oracleText = "If a source you control would deal noncombat damage to an opponent or a permanent " +
        "an opponent controls, it deals double that damage to that player or permanent instead.\n" +
        "{1}{R/P}{R/P}, Discard two cards: Put an indestructible counter on Solphim. " +
        "({R/P} can be paid with either {R} or 2 life.)"

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.OpponentOrPermanentTheyControl,
                source = GameObjectFilter.Any.youControl(),
                damageType = DamageType.NonCombat,
            )
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R/P}{R/P}"),
            Costs.Discard(count = 2),
        )
        effect = Effects.AddCounters(CounterType.INDESTRUCTIBLE, 1, EffectTarget.Self)
        description = "{1}{R/P}{R/P}, Discard two cards: Put an indestructible counter on Solphim."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "150"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3a0c7f3-7fb9-43de-a9af-96532c31e5ed.jpg?1783918023"
    }
}
