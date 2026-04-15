package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DealDamagePerEntityInZoneEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dragonhawk, Fate's Tempest {3}{R}{R}
 * Legendary Creature — Bird Dragon
 * 5/5
 *
 * Flying
 * Whenever Dragonhawk enters or attacks, exile the top X cards of your library,
 * where X is the number of creatures you control with power 4 or greater. You may
 * play those cards until your next end step. At the beginning of your next end step,
 * Dragonhawk deals 2 damage to each opponent for each of those cards that are still exiled.
 */
val DragonhawkFatesTempest = card("Dragonhawk, Fate's Tempest") {
    manaCost = "{3}{R}{R}"
    typeLine = "Legendary Creature — Bird Dragon"
    oracleText = "Flying\nWhenever Dragonhawk enters or attacks, exile the top X cards of your library, where X is the number of creatures you control with power 4 or greater. You may play those cards until your next end step. At the beginning of your next end step, Dragonhawk deals 2 damage to each opponent for each of those cards that are still exiled."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)

    val impulseDrawEffect: Effect = CompositeEffect(listOf(
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(
                DynamicAmount.AggregateBattlefield(
                    Player.You,
                    GameObjectFilter.Creature.powerAtLeast(4)
                )
            ),
            storeAs = "exiledCards"
        ),
        MoveCollectionEffect(
            from = "exiledCards",
            destination = CardDestination.ToZone(Zone.EXILE)
        ),
        GrantMayPlayFromExileEffect("exiledCards"),
        CreateDelayedTriggerEffect(
            step = Step.END,
            fireOnlyOnControllersTurn = true,
            onControllerNextTurn = true,
            effect = DealDamagePerEntityInZoneEffect(
                collectionName = "exiledCards",
                zone = Zone.EXILE,
                damagePerEntity = 2,
                target = EffectTarget.PlayerRef(Player.EachOpponent),
                damageSource = EffectTarget.Self
            )
        )
    ))

    // When Dragonhawk enters the battlefield
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = impulseDrawEffect
    }

    // Whenever Dragonhawk attacks
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = impulseDrawEffect
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "132"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8659789c-6a2c-439f-a348-b9b1b06c55b8.jpg?1721426605"
    }
}
