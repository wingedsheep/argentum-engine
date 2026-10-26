package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnceOnlyAbilityKind
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dragonkin Berserker — Kaldheim #131 (canonical printing)
 * {1}{R} · Creature — Human Berserker · 2/2
 *
 * First strike
 * Boast abilities you activate cost {1} less to activate for each Dragon you control.
 * Boast — {4}{R}: Create a 5/5 red Dragon creature token with flying.
 *
 * The discount is [ReduceActivatedAbilityCost] over every ability you activate
 * ([ActivatedAbilityReach.ACTIVATED_BY_YOU]) narrowed to [OnceOnlyAbilityKind.BOAST], so it reaches
 * the boasts of your other creatures too. Generic-only (CR 118.7): four Dragons bring this boast
 * down to {R}, never below.
 */
val DragonkinBerserker = card("Dragonkin Berserker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Berserker"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "Boast abilities you activate cost {1} less to activate for each Dragon you control.\n" +
        "Boast — {4}{R}: Create a 5/5 red Dragon creature token with flying. (Activate only if " +
        "this creature attacked this turn and only once each turn.)"

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Any),
            amount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype(Subtype.DRAGON)).count(),
            reach = ActivatedAbilityReach.ACTIVATED_BY_YOU,
            onceOnlyKind = OnceOnlyAbilityKind.BOAST,
        )
    }

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{4}{R}")
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7f22fe4-2390-4c3d-8b5e-0cf398d97f6a.jpg?1783928231"
        ruling("2021-02-05", "The cost reduction applies only to generic mana in the activation costs of boast abilities you activate. It can't reduce requirements of specific colors of mana.")
        ruling("2021-02-05", "Once you announce that you're activating a boast ability, no player may take actions until the ability has been paid for. Notably, opponents can't try to change the activation cost by removing your Dragons.")
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
    }
}
