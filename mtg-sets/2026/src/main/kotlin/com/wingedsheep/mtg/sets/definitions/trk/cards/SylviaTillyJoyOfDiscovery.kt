package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sylvia Tilly, Joy of Discovery — Star Trek #217
 * {2}{G} · Legendary Creature — Human Scientist Artificer · 2/2
 *
 * When Sylvia Tilly enters, surveil 2.
 * At the beginning of combat on your turn, you may pay {1} and exile a card from your graveyard.
 * When you do, put a +1/+1 counter on target creature.
 *
 * The combat trigger is a CR 603.12 reflexive trigger whose optional action is the compound
 * "pay {1} and exile a card" (Three Dog, Galaxy News DJ's mana-then-other-cost shape, with Kishla
 * Trawlers' non-targeting graveyard exile). The reflexive executor scores both halves before asking,
 * so with an empty graveyard or no spare mana the "may" isn't offered and nothing is paid. The
 * counter's target is chosen only after the action is done, as the reflexive trigger goes on the stack.
 */
val SylviaTillyJoyOfDiscovery = card("Sylvia Tilly, Joy of Discovery") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Scientist Artificer"
    power = 2
    toughness = 2
    oracleText = "When Sylvia Tilly enters, surveil 2. (Look at the top two cards of your library, then " +
        "put any number of them into your graveyard and the rest on top of your library in any order.)\n" +
        "At the beginning of combat on your turn, you may pay {1} and exile a card from your graveyard. " +
        "When you do, put a +1/+1 counter on target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Surveil(2)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}") then Effects.Pipeline {
                val exiled = selectTarget(
                    TargetObject(filter = TargetFilter.CardInGraveyard.ownedByYou()),
                    nonTargeting = true,
                    prompt = "Choose a card in your graveyard to exile"
                )
                run(Effects.Exile(exiled.asTarget))
            },
            optional = true,
            descriptionOverride = "You may pay {1} and exile a card from your graveyard. " +
                "When you do, put a +1/+1 counter on target creature."
        ) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        }
        description = "At the beginning of combat on your turn, you may pay {1} and exile a card from " +
            "your graveyard. When you do, put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Ron Lemen"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe6e03b5-cd96-430b-9c74-96eca0212a81.jpg?1792989779"
    }
}
