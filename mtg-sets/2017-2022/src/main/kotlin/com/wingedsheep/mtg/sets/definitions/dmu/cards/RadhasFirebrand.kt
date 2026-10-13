package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Radha's Firebrand
 * {1}{R}
 * Creature — Human Warrior
 * 3/1
 * Whenever this creature attacks, target creature defending player controls with power less than
 * this creature's power can't block this turn.
 * Domain — {5}{R}: This creature gets +2/+2 until end of turn. This ability costs {1} less to
 * activate for each basic land type among lands you control. Activate only once each turn.
 *
 * The attack trigger's target filter compares against this creature's projected power
 * (`powerLessThanEntity(Self)`, Rangers of Ithilien's shape). The discount rides
 * `genericCostReduction` fed by `DynamicAmounts.domain()` (Jodah's Codex's shape), so only the
 * generic {5} shrinks — the {R} always remains.
 */
val RadhasFirebrand = card("Radha's Firebrand") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature attacks, target creature defending player controls with power less than this creature's power can't block this turn.\n" +
        "Domain — {5}{R}: This creature gets +2/+2 until end of turn. This ability costs {1} less to activate for each basic land type among lands you control. Activate only once each turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val victim = target(
            TargetFilter(
                GameObjectFilter.Creature.defendingPlayerControls().powerLessThanEntity(EffectTarget.Self)
            )
        )
        effect = Effects.CantBlock(victim)
    }

    activatedAbility {
        cost = Costs.Mana("{5}{R}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        genericCostReduction = DynamicAmounts.domain()
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Domain — {5}{R}: This creature gets +2/+2 until end of turn. This ability costs " +
            "{1} less to activate for each basic land type among lands you control. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Ângelo Bortolini"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae8a90c4-4a79-49e7-9fab-076b35d28adb.jpg?1783921311"
    }
}
