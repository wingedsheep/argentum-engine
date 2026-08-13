package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Azog, Moria's Ruin — The Hobbit #61
 * {2}{B} · Legendary Creature — Goblin Soldier · 1/3
 *
 * The target's projected power is frozen before it is destroyed, so the amass amount uses the
 * creature's last-known power even after it leaves the battlefield. The relational player loop
 * then resolves under that creature's last-known controller; this makes that player choose an
 * Army when they control several. The outer condition is deliberately evaluated before the
 * destruction so the final draw remembers whether Azog's controller controlled the creature.
 */
val AzogMoriasRuin = card("Azog, Moria's Ruin") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Goblin Soldier"
    oracleText = "When Azog enters, destroy up to one other target creature. Its controller " +
        "amasses Goblins X, where X is that creature's power. If you controlled that creature, " +
        "draw a card. (To amass Goblins X, that player puts X +1/+1 counters on an Army they " +
        "control. It's also a Goblin. If they don't control an Army, they create a 0/0 black " +
        "Goblin Army creature token first.)"
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one other target creature",
            TargetCreature(optional = true, filter = TargetFilter.OtherCreature),
        )

        val resolveForItsController = Effects.StoreNumber(
            name = "azogTargetPower",
            amount = DynamicAmounts.targetPower(),
        ).then(
            Effects.Destroy(creature),
        ).then(
            Effects.ForEachPlayer(
                Player.ControllerOf("that creature"),
                listOf(
                    Effects.Amass(DynamicAmount.VariableReference("azogTargetPower"), "Goblin"),
                ),
            ),
        )

        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.youControl()),
            effect = resolveForItsController.then(Effects.DrawCards(1)),
            elseEffect = resolveForItsController,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Miklós Ligeti"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/135da718-affc-46ba-be57-c12c23b54dad.jpg?1784798028"
        ruling("2026-06-29", "If no target is chosen for Azog's ability, \"its controller\" is undefined and no player amasses Goblins.")
        ruling("2026-06-29", "Use the power of the creature as it last existed on the battlefield to determine the value of X.")
    }
}
