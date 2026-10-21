package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nahiri's Resolve
 * {3}{R}{W}
 * Enchantment
 * Creatures you control get +1/+0 and have haste.
 * At the beginning of your end step, exile any number of nontoken artifacts and/or creatures you
 * control. Return those cards to the battlefield under their owner's control at the beginning of
 * your next upkeep.
 *
 * The end-step ability doesn't target: the choice is made on resolution among the nontoken
 * artifacts and creatures you control, picked on the battlefield. The exile is a tracked move, and
 * the upkeep trigger carries that pile (`carryCollections`), so only cards that actually reached
 * exile — and are still there — come back, even if Nahiri's Resolve has left the battlefield.
 * `fireOnPlayer = You` keeps the return on *your* next upkeep, not the opponent's.
 */
val NahirisResolve = card("Nahiri's Resolve") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+0 and have haste.\n" +
        "At the beginning of your end step, exile any number of nontoken artifacts and/or creatures " +
        "you control. Return those cards to the battlefield under their owner's control at the " +
        "beginning of your next upkeep."

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = GroupFilter.AllCreaturesYouControl)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val candidates = gather(
                (GameObjectFilter.Artifact or GameObjectFilter.Creature).nontoken(),
                player = Player.You
            )
            val chosen = chooseAnyNumber(
                from = candidates,
                prompt = "Exile any number of nontoken artifacts and/or creatures you control " +
                    "(they return at the beginning of your next upkeep)",
                useTargetingUI = true
            )
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE), name = "nahirisResolveExiled")
            run(Effects.CreateDelayedTrigger(
                step = Step.UPKEEP,
                fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                effect = Effects.Pipeline {
                    move(
                        exiled,
                        CardDestination.ToZone(Zone.BATTLEFIELD),
                        underOwnersControl = true
                    )
                },
                carryCollections = listOf(exiled.key)
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Alexey Kruglov"
        flavorText = "\"Zendikar is *my* home!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df9acf30-b4f4-4e99-9763-a3055e91fefb.jpg?1783916513"
    }
}
