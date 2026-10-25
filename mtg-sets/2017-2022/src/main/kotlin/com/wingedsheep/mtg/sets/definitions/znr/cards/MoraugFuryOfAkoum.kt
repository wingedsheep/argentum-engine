package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Moraug, Fury of Akoum — Zendikar Rising #150 (canonical printing)
 * {4}{R}{R} · Legendary Creature — Minotaur Warrior · 6/6
 *
 * Each creature you control gets +1/+0 for each time it has attacked this turn.
 * Landfall — Whenever a land you control enters, if it's your main phase, there's an additional
 * combat phase after this phase. At the beginning of that combat, untap all creatures you control.
 *
 * The anthem is [GrantDynamicStats] fed each affected creature's own
 * [EntityNumericProperty.TimesAttackedThisTurn] through [EffectTarget.AffectedEntity]: one attack
 * per combat phase it was declared in, counted from before Moraug arrived, reset for a creature that
 * changed zones.
 *
 * "If it's your main phase" is an intervening if (checked on trigger and on resolution). The added
 * phase is combat only — the rulings say no main phase follows it — and its untap is the
 * [Effects.AddCombatPhaseWithBeginningTrigger] rider, linked to that one added combat: cast in the
 * precombat main phase the extra combat comes before the regular one and only it untaps; two
 * resolutions give two combats that each untap at their own beginning.
 */
val MoraugFuryOfAkoum = card("Moraug, Fury of Akoum") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Minotaur Warrior"
    power = 6
    toughness = 6
    oracleText = "Each creature you control gets +1/+0 for each time it has attacked this turn.\n" +
        "Landfall — Whenever a land you control enters, if it's your main phase, there's an additional " +
        "combat phase after this phase. At the beginning of that combat, untap all creatures you control."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            powerBonus = DynamicAmounts.propertyOf(
                EffectTarget.AffectedEntity,
                EntityNumericProperty.TimesAttackedThisTurn
            ),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        interveningIf = Conditions.IsYourMainPhase
        effect = Effects.AddCombatPhaseWithBeginningTrigger(
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.Untap(EffectTarget.IterationEntity)
            )
        )
        description = "Landfall — Whenever a land you control enters, if it's your main phase, there's an " +
            "additional combat phase after this phase. At the beginning of that combat, untap all " +
            "creatures you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "150"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc5eacd7-aaa7-4720-9794-52e7b098c82c.jpg?1783929359"
        ruling(
            "2020-09-25",
            "Moraug's first ability checks each turn how many times each creature has attacked. It counts " +
                "times that creature attacked before Moraug was on the battlefield, and it stops applying if " +
                "Moraug leaves the battlefield."
        )
        ruling(
            "2020-09-25",
            "Moraug's first ability counts how many times a creature has attacked, not how many times it has " +
                "dealt damage. The first time a creature attacks in a turn, the +1/+0 bonus will apply to the " +
                "combat damage it deals during that combat."
        )
        ruling(
            "2020-09-25",
            "A creature that enters the battlefield attacking hasn't attacked that combat. Moraug's ability " +
                "won't count that combat. Unless that creature has haste, it won't be able to attack in " +
                "additional combats this turn."
        )
        ruling(
            "2020-09-25",
            "There won't be an additional main phase after the additional combat phase. For example, if the " +
                "landfall ability resolves twice during your postcombat main phase, you'll get two consecutive " +
                "additional combat phases after your main phase (untapping your creatures at the beginning of " +
                "each), followed by your ending phase."
        )
        ruling(
            "2020-09-25",
            "If the landfall ability resolves during your precombat main phase, the additional combat phase " +
                "will happen before your regular combat phase. You'll untap creatures you control at the " +
                "beginning of the additional combat but not at the beginning of your regular combat."
        )
    }
}
