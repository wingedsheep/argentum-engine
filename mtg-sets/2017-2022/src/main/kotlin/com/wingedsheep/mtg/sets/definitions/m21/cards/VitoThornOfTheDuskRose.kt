package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vito, Thorn of the Dusk Rose — Core Set 2021 #127
 * {2}{B} · Legendary Creature — Vampire Cleric · 1/3
 *
 * Whenever you gain life, target opponent loses that much life.
 * {3}{B}{B}: Creatures you control gain lifelink until end of turn.
 *
 * "That much" is the amount gained by the triggering life-gain event
 * ([DynamicAmounts.triggerLifeGained]) — the same shape as Enduring Tenacity. Each life-gain event
 * triggers separately, so two lifelinkers dealing combat damage at once trigger it twice.
 * The lifelink grant iterates the group locked in on resolution (CR 611.2c).
 */
val VitoThornOfTheDuskRose = card("Vito, Thorn of the Dusk Rose") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire Cleric"
    power = 1
    toughness = 3
    oracleText = "Whenever you gain life, target opponent loses that much life.\n" +
        "{3}{B}{B}: Creatures you control gain lifelink until end of turn."

    triggeredAbility {
        val opponent = target(Targets.Opponent)
        trigger = Triggers.you.gainsLife()
        effect = Effects.LoseLife(DynamicAmounts.triggerLifeGained(), opponent)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{B}{B}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.IterationEntity)
        )
        description = "Creatures you control gain lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Lie Setiawan"
        flavorText = "\"Through the blood of the wicked, we shall be redeemed. Let us pray.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fe79ee4-c3f3-4a6b-a967-203ca3b70ee5.jpg?1783930698"
        ruling("2020-06-23", "Because Vito's first ability doesn't deal damage, you won't gain life when it resolves if Vito has lifelink.")
        ruling(
            "2020-06-23",
            "If an ability triggers whenever an opponent loses life and causes you to gain life, such as the ability of Exquisite Blood, " +
                "this will loop until either you win the game or a player takes an action to break the loop."
        )
        ruling(
            "2020-06-23",
            "Each creature with lifelink dealing combat damage causes a separate life-gaining event. For example, if two creatures you " +
                "control with lifelink deal combat damage at the same time, Vito's first ability will trigger twice and you may choose a " +
                "different opponent for each trigger. However, if a single creature you control with lifelink deals combat damage to " +
                "multiple creatures, players, and/or planeswalkers at the same time (perhaps because it has trample or was blocked by more " +
                "than one creature), the ability will trigger only once."
        )
        ruling("2020-06-23", "If you gain an amount of life \"for each\" of something, that life is gained as one event and Vito's first ability triggers only once.")
        ruling(
            "2020-06-23",
            "In a Two-Headed Giant game, life gained by your teammate won't cause the ability to trigger, even though it caused your team's life total to increase."
        )
    }
}
