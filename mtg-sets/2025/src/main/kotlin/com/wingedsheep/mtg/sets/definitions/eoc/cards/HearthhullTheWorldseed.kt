package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hearthhull, the Worldseed — Edge of Eternities Commander #1 (canonical printing)
 * {1}{B}{R}{G} · Legendary Artifact — Spacecraft · 6/7
 *
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)
 * 2+ | {1}, {T}, Sacrifice a land: Draw two cards. You may play an additional land this turn.
 * 8+ | Flying, vigilance, haste
 * Whenever you sacrifice a land, each opponent loses 2 life.
 *
 * The 2+ ability is gated on the charge-counter threshold (Uthros, Titanic Godcore's shape); the
 * 8+ tier grants the creature type and keywords. The land-sacrifice drain is not a station tier,
 * so it works at any charge count — including off the 2+ ability's own sacrifice cost.
 */
val HearthhullTheWorldseed = card("Hearthhull, the Worldseed") {
    manaCost = "{1}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 6
    toughness = 7
    oracleText = "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n" +
        "2+ | {1}, {T}, Sacrifice a land: Draw two cards. You may play an additional land this turn.\n" +
        "8+ | Flying, vigilance, haste\n" +
        "Whenever you sacrifice a land, each opponent loses 2 life."

    station()

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Land))
        effect = Effects.DrawCards(2) then Effects.PlayAdditionalLands(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 2)
            )
        )
        description = "{1}, {T}, Sacrifice a land: Draw two cards. You may play an additional land this turn."
    }

    val charge8 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)

    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.VIGILANCE.name, GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.HASTE.name, GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Land)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever you sacrifice a land, each opponent loses 2 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b913e79-a9f6-44d4-bb37-c4f6f66b8151.jpg?1783906067"

        ruling(
            "2025-07-25",
            "The effect of Hearthhull's second ability is cumulative with similar effects. For example, if you " +
                "control Icetill Explorer and you activate Hearthhull's ability, you'll be able to play three lands on that turn."
        )
        ruling(
            "2025-07-25",
            "Hearthhull's second ability doesn't allow you to play lands on other players' turns."
        )
    }
}
