package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Desert Were-Worm — The Hobbit #92
 * {4}{R}{R} · Creature — Dragon Wurm · Rare
 * 0/5
 *
 * This creature gets +2/+0 for each Mountain you control.
 * Whenever you attack with creatures with total power 12 or greater for the first time each turn,
 * untap all attacking creatures. After this phase, there is an additional combat phase.
 *
 * Modeling notes:
 *  - The Mountain pump is a static [GrantDynamicStatsEffect] on the source, so it feeds the total
 *    power the attack trigger measures — a Were-Worm swinging alongside six Mountains is already
 *    12 power by itself.
 *  - "For the first time each turn" is `oncePerTurn` on the ability paired with an
 *    intervening-if [Compare] on the attackers' total power (CR 603.4). Because the condition is
 *    checked before the ability triggers, an under-12 attack in an earlier combat doesn't spend
 *    the turn's single use — the trigger still fires on a later combat that does reach 12.
 *  - "Untap all attacking creatures" is every attacker on the battlefield, not just yours, so the
 *    group filter is unscoped by controller.
 *  - "After this phase, there is an additional combat phase" is [Effects.AddCombatPhase] alone —
 *    combat only, with no trailing main phase (CR 500.8).
 */
val DesertWereWorm = card("Desert Were-Worm") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon Wurm"
    power = 0
    toughness = 5
    oracleText = "This creature gets +2/+0 for each Mountain you control.\n" +
        "Whenever you attack with creatures with total power 12 or greater for the first time " +
        "each turn, untap all attacking creatures. After this phase, there is an additional " +
        "combat phase."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.Multiply(
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)
                ).count(),
                2
            ),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.YouAttack
        triggerCondition = Compare(
            left = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Creature.attacking(),
                aggregation = Aggregation.SUM,
                property = CardNumericProperty.POWER
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(12)
        )
        oncePerTurn = true
        effect = Effects.Composite(
            listOf(
                Effects.ForEachInGroup(
                    filter = GroupFilter(baseFilter = GameObjectFilter.Creature.attacking()),
                    effect = TapUntapEffect(EffectTarget.Self, tap = false)
                ),
                Effects.AddCombatPhase
            )
        )
        description = "Whenever you attack with creatures with total power 12 or greater for the " +
            "first time each turn, untap all attacking creatures. After this phase, there is an " +
            "additional combat phase."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Aldo Domínguez"
        flavorText = "Fight wild Were-worms in the Last Desert\n" +
            "—Expression meaning \"an impossible task\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc12c22a-11ff-4fb0-bc42-dd8490b8efb7.jpg?1784733924"
    }
}
