package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Betor, Ancestor's Voice — Tarkir: Dragonstorm Commander #1
 * {2}{W}{B}{G} · Legendary Creature — Spirit Dragon · 3/5
 *
 * Flying, lifelink
 * At the beginning of your end step, put a number of +1/+1 counters on up to one other target
 * creature you control equal to the amount of life you gained this turn. Return up to one target
 * creature card with mana value less than or equal to the amount of life you lost this turn from
 * your graveyard to the battlefield.
 *
 * Two independent "up to one" slots (the Winterthorn Blessing shape). The counter count reads
 * `lifeGainedThisTurn` at resolution; the reanimation slot's mana-value cap is the dynamic
 * `manaValueAtMostDynamic(lifeLostThisTurn)` (the Moseo shape), checked when the target is chosen
 * and again on resolution. Neither half is gated: with no life gained the first slot places zero
 * counters, and with no life lost only a mana value 0 creature card qualifies.
 */
val BetorAncestorsVoice = card("Betor, Ancestor's Voice") {
    manaCost = "{2}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 3
    toughness = 5
    oracleText = "Flying, lifelink\n" +
        "At the beginning of your end step, put a number of +1/+1 counters on up to one other target " +
        "creature you control equal to the amount of life you gained this turn. Return up to one target " +
        "creature card with mana value less than or equal to the amount of life you lost this turn from " +
        "your graveyard to the battlefield."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val creature = target(TargetFilter.OtherCreatureYouControl, optional = true)
        val graveyardCard = target(
            TargetFilter(
                GameObjectFilter.Creature.ownedByYou()
                    .manaValueAtMostDynamic(DynamicAmounts.lifeLostThisTurn(Player.You)),
                zone = Zone.GRAVEYARD,
            ),
            optional = true,
        )
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.lifeGainedThisTurn(Player.You),
            creature,
        ) then Effects.PutOntoBattlefield(graveyardCard)
        description = "At the beginning of your end step, put a number of +1/+1 counters on up to one " +
            "other target creature you control equal to the amount of life you gained this turn. Return " +
            "up to one target creature card with mana value less than or equal to the amount of life you " +
            "lost this turn from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e261489-8dde-4594-8868-69f432f03d03.jpg?1783907191"
    }
}
