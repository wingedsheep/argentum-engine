package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brash Taunter — Core Set 2021 #133
 * {4}{R} · Creature — Goblin · 1/1
 *
 * Indestructible
 * Whenever this creature is dealt damage, it deals that much damage to target opponent.
 * {2}{R}, {T}: This creature fights another target creature.
 *
 * The damage trigger is Barbed Servitor's `Triggers.self.isDealtDamage()` reading the full
 * amount dealt (`triggerDamageAmount`, not capped at toughness — ruling), with Brash Taunter
 * itself as the damage source. The fight is the `Effects.Fight(Self, target)` shape.
 */
val BrashTaunter = card("Brash Taunter") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1
    oracleText = "Indestructible\n" +
        "Whenever this creature is dealt damage, it deals that much damage to target opponent.\n" +
        "{2}{R}, {T}: This creature fights another target creature."

    keywords(Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(DynamicAmounts.triggerDamageAmount(), opponent)
        description = "Whenever this creature is dealt damage, it deals that much damage to target opponent."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        val foe = target(TargetFilter.OtherCreature)
        effect = Effects.Fight(EffectTarget.Self, foe)
        description = "{2}{R}, {T}: This creature fights another target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Svetlin Velinov"
        flavorText = "\"You hit like a kobold!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebb5e613-a803-42f3-840a-7089ac6b7e3d.jpg?1783930695"
        ruling(
            "2020-06-23",
            "A creature can be dealt an amount of damage greater than its toughness. For example, if Brash " +
                "Taunter is dealt 3 damage, its middle ability deals 3 damage, not 1, to the target opponent."
        )
        ruling(
            "2020-06-23",
            "If the target creature is an illegal target when Brash Taunter's last ability tries to resolve, " +
                "the ability doesn't resolve. If Brash Taunter is no longer on the battlefield, the target " +
                "creature won't deal or be dealt damage."
        )
    }
}
