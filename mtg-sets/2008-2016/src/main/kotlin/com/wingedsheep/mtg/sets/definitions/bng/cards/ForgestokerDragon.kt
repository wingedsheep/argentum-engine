package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Forgestoker Dragon
 * {4}{R}{R}
 * Creature — Dragon
 * 5/4
 *
 * Flying
 * {1}{R}: This creature deals 1 damage to target creature. That creature can't block this combat.
 * Activate only if this creature is attacking.
 *
 * The damage and the block restriction share one bound target (a `then` chain, as on Blood
 * Aspirant); "this combat" is [Duration.EndOfCombat], not end of turn. The attacking gate is
 * [ActivationRestriction.OnlyIfCondition] over [Conditions.SourceIsAttacking] (Glint-Horn
 * Buccaneer's shape).
 */
val ForgestokerDragon = card("Forgestoker Dragon") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 4
    oracleText = "Flying\n" +
        "{1}{R}: This creature deals 1 damage to target creature. That creature can't block this combat. " +
        "Activate only if this creature is attacking."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.SourceIsAttacking))
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(1, creature) then Effects.CantBlock(creature, Duration.EndOfCombat)
        description = "{1}{R}: This creature deals 1 damage to target creature. That creature can't block " +
            "this combat. Activate only if this creature is attacking."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Todd Lockwood"
        flavorText = "The Akroans fashion their helms to honor the dragons, not to protect against them, " +
            "as that would be of little help."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fd44170-ded7-4895-8950-bfa9d19e471d.jpg?1783939545"
        ruling(
            "2014-02-01",
            "Forgestoker Dragon's activated ability can target any creature, not just creatures controlled " +
                "by the defending player or ones that could block."
        )
        ruling(
            "2014-02-01",
            "If you don't want the target creature to be able to block, Forgestoker Dragon's activated " +
                "ability must be activated during the declare attackers step."
        )
    }
}
