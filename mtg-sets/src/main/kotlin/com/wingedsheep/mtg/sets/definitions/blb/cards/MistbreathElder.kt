package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mistbreath Elder {G}
 * Creature — Frog Warrior
 * 2/2
 *
 * At the beginning of your upkeep, return another creature you control to its
 * owner's hand. If you do, put a +1/+1 counter on this creature. Otherwise,
 * you may return this creature to its owner's hand.
 */
val MistbreathElder = card("Mistbreath Elder") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Frog Warrior"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your upkeep, return another creature you control to its owner's hand. If you do, put a +1/+1 counter on this creature. Otherwise, you may return this creature to its owner's hand."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = ConditionalEffect(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.youControl(), excludeSelf = true),
            // If you control another creature: bounce one of them (Gather → Select → Move;
            // the battlefield→hand move routes to the owner's hand), then counter self.
            effect = Effects.Composite(
                GatherCardsEffect(
                    source = CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Creature,
                        player = Player.You,
                        excludeSelf = true
                    ),
                    storeAs = "bounceCandidates"
                ),
                SelectFromCollectionEffect(
                    from = "bounceCandidates",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "bounced",
                    prompt = "Return another creature you control to its owner's hand",
                    useTargetingUI = true
                ),
                MoveCollectionEffect(
                    from = "bounced",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            ),
            // Otherwise: you may return this creature to hand
            elseEffect = MayEffect(Effects.ReturnToHand(EffectTarget.Self))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5246540-5a84-41d8-9e30-8e7a6c0e84e1.jpg?1721426876"
        ruling("2024-07-26", "If you control creatures other than Mistbreath Elder as its triggered ability resolves, you must return one of them to its owner's hand. If you don't, you have the option to return Mistbreath Elder to its owner's hand or leave it on the battlefield and have the ability do nothing.")
    }
}
