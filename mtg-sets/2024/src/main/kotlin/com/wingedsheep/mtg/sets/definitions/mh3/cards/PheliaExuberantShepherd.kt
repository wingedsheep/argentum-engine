package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phelia, Exuberant Shepherd
 * {1}{W}
 * Legendary Creature — Dog
 * 2/2
 * Flash
 * Whenever Phelia attacks, exile up to one other target nonland permanent. At the beginning of the
 * next end step, return that card to the battlefield under its owner's control. If it entered under
 * your control, put a +1/+1 counter on Phelia.
 *
 * The chosen target is gathered and exiled, and the delayed end-step trigger carries that pile
 * (`carryCollections`, CR 603.7c "that card"): a card that has since left exile — or a token that
 * ceased to exist there — is dropped when it fires, so nothing returns and no counter is placed.
 * The return is a *tracked* move, so "if it entered" reads only a card that actually reached the
 * battlefield; filtering that by "you control" covers both "you own it" and a control-changing
 * replacement. The counter goes on Phelia only if she is still on the battlefield; the card still
 * returns if she isn't (2024-06-07 ruling).
 */
val PheliaExuberantShepherd = card("Phelia, Exuberant Shepherd") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dog"
    power = 2
    toughness = 2
    oracleText = "Flash\nWhenever Phelia attacks, exile up to one other target nonland permanent. " +
        "At the beginning of the next end step, return that card to the battlefield under its " +
        "owner's control. If it entered under your control, put a +1/+1 counter on Phelia."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        target(TargetFilter.OtherNonlandPermanent, optional = true)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.ChosenTargets)
            exile(exiled)
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Pipeline {
                    val returned = moveTracked(
                        exiled,
                        CardDestination.ToZone(Zone.BATTLEFIELD),
                        underOwnersControl = true
                    )
                    ifNotEmpty(returned, filter = GameObjectFilter.Any.youControl()) {
                        run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
                    }
                },
                carryCollections = listOf(exiled.key)
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55707746-da6e-46e5-a5ca-7ac843fdc38e.jpg?1783911298"
        ruling("2024-06-07", "The exiled card will return to the battlefield at the beginning of the end step even if Phelia is no longer on the battlefield.")
        ruling("2024-06-07", "If the permanent that returns to the battlefield has any abilities that trigger at the beginning of the end step, those abilities won't trigger that turn.")
        ruling("2024-06-07", "Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to the exiled permanent will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist. Once the exiled permanent returns, it's considered a new object with no relation to the object that it was.")
        ruling("2024-06-07", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
