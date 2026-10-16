package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Motherlode, Excavator
 * {3}{R}{R}
 * Legendary Artifact Creature — Robot
 * 5/5
 *
 * When The Motherlode enters, choose target opponent. You get an amount of {E} (energy counters)
 * equal to the number of nonbasic lands that player controls.
 * Whenever The Motherlode attacks, you may pay {E}{E}{E}{E}. When you do, destroy target nonbasic
 * land defending player controls, and creatures that player controls without flying can't block
 * this turn.
 *
 * The reflexive trigger (CR 603.12) chooses its land as it goes on the stack. "That player" is
 * the defending player, fixed when the reflexive ability resolves — the can't-block group's
 * player reference is baked then, so it holds even if The Motherlode leaves combat — while the
 * set of their creatures without flying stays live for the turn (ruling).
 */
val TheMotherlodeExcavator = card("The Motherlode, Excavator") {
    manaCost = "{3}{R}{R}"
    typeLine = "Legendary Artifact Creature — Robot"
    power = 5
    toughness = 5
    oracleText = "When The Motherlode enters, choose target opponent. You get an amount of {E} " +
        "(energy counters) equal to the number of nonbasic lands that player controls.\n" +
        "Whenever The Motherlode attacks, you may pay {E}{E}{E}{E}. When you do, destroy target " +
        "nonbasic land defending player controls, and creatures that player controls without " +
        "flying can't block this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.AddDynamicCounters(
            CounterType.ENERGY,
            DynamicAmounts.battlefield(opponent.asPlayer, GameObjectFilter.NonbasicLand).count(),
            EffectTarget.Controller
        )
        description = "When The Motherlode enters, choose target opponent. You get an amount of {E} " +
            "equal to the number of nonbasic lands that player controls."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayExactCounters(CounterType.ENERGY, 4),
            optional = true,
            descriptionOverride = "You may pay {E}{E}{E}{E}. When you do, destroy target nonbasic land " +
                "defending player controls, and creatures that player controls without flying can't " +
                "block this turn."
        ) {
            val defending = EffectTarget.PlayerRef(Player.DefendingPlayer)
            val land = target(TargetFilter(GameObjectFilter.NonbasicLand.targetPlayerControls(defending)))
            effect = Effects.Destroy(land) then Effects.CantBlockGroup(
                GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING).targetPlayerControls(defending))
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Josu Solano"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b9df394-522c-4458-bc36-cf24ad763718.jpg?1783912403"
        ruling(
            "2024-03-08",
            "Use the number of nonbasic lands the target opponent controls when The Motherlode's first " +
                "ability resolves to determine the number of energy counters you get."
        )
        ruling(
            "2024-03-08",
            "You don't choose a target nonbasic land at the time The Motherlode's last ability triggers. " +
                "Rather, a second \"reflexive\" ability triggers when you pay {E}{E}{E}{E} this way. You " +
                "choose a target for that ability as it goes on the stack. Each player may respond to this " +
                "triggered ability as normal."
        )
        ruling(
            "2024-03-08",
            "The Motherlode's reflexive triggered ability prevents all creatures without flying that player " +
                "controls from blocking that turn, including creatures that lose flying after the ability " +
                "resolves as well as creatures without flying that enter the battlefield or come under that " +
                "player's control later in the turn."
        )
        ruling(
            "2024-03-08",
            "If the target nonbasic land is an illegal target as The Motherlode's reflexive triggered ability " +
                "tries to resolve, it won't resolve and none of its effects happen. Creatures defending player " +
                "controls without flying will still be able to block this turn."
        )
    }
}
