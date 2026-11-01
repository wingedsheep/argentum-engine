package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.times

/**
 * The Elderspell
 * {B}{B}
 * Sorcery
 * Destroy any number of target planeswalkers. Choose a planeswalker you control. Put two loyalty
 * counters on it for each planeswalker destroyed this way.
 *
 * "Any number of target planeswalkers" is `unlimited = true` (zero targets is a legal cast, per
 * ruling). The targets are destroyed with `moveTracked(…, MoveType.Destroy)` — Builder's Bane's
 * shape — so an indestructible or regenerated planeswalker drops out of the tracked set and isn't
 * counted, while one destroyed but redirected elsewhere by a replacement still counts. The
 * recipient is chosen at resolution, after the destruction (so it's among the survivors), from the
 * planeswalkers you control — a choice, not a target (Reckoner Shakedown's shape). Controlling none
 * means no counters are placed; the destruction still happens.
 */
val TheElderspell = card("The Elderspell") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy any number of target planeswalkers. Choose a planeswalker you control. " +
        "Put two loyalty counters on it for each planeswalker destroyed this way."

    spell {
        targets(TargetFilter.Planeswalker, unlimited = true)
        effect = Effects.Pipeline {
            val victims = gather(CardSource.ChosenTargets)
            val destroyed = moveTracked(
                victims,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
            val loyalty = storeNumber(destroyed.count * 2)
            val mine = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Planeswalker))
            val recipient = chooseExactly(
                1,
                from = mine,
                prompt = "Choose a planeswalker you control to put loyalty counters on",
                useTargetingUI = true
            )
            run(Effects.AddCountersToCollection(recipient, CounterType.LOYALTY, loyalty.amount))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Daarken"
        flavorText = "The path to power is often paved with atrocities."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3b16625-7faf-4de6-abf7-d397988e32fb.jpg?1783933446"
        ruling(
            "2019-05-03",
            "You choose the target planeswalkers to destroy as you cast The Elderspell, but you don't " +
                "choose a planeswalker to receive loyalty counters until The Elderspell is resolving."
        )
        ruling(
            "2019-05-03",
            "You can cast The Elderspell without controlling any planeswalkers. The target planeswalkers " +
                "will still be destroyed. You can also cast it without targeting any planeswalkers if you " +
                "really want to do so."
        )
    }
}
