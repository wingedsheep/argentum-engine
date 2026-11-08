package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Hope of Ghirapur
 * {1}
 * Legendary Artifact Creature — Thopter
 * 1/1
 * Flying
 * Sacrifice Hope of Ghirapur: Until your next turn, target player who was dealt combat damage by
 * Hope of Ghirapur this turn can't cast noncreature spells.
 *
 * The target restriction reads the source's combat-damage stamp (`DamageType.Combat`), which the
 * Hope keeps as last-known information after the sacrifice cost moves it to the graveyard, so the
 * target is still legal on resolution. Spells cast in response, and spells cast before activation,
 * are unaffected (per the rulings) — the ban only stops later casts.
 */
val HopeOfGhirapur = card("Hope of Ghirapur") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Thopter"
    power = 1
    toughness = 1
    oracleText = "Flying\nSacrifice Hope of Ghirapur: Until your next turn, target player who was dealt " +
        "combat damage by Hope of Ghirapur this turn can't cast noncreature spells."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.SacrificeSelf
        val player = target(
            TargetPlayer(
                restriction = Conditions.candidateWasDealtDamageBySourceThisTurn(DamageType.Combat),
                descriptionOverride = "target player who was dealt combat damage by Hope of Ghirapur this turn",
            )
        )
        effect = Effects.CantCastSpells(
            target = player,
            duration = Duration.UntilYourNextTurn,
            spellFilter = GameObjectFilter.Noncreature,
        )
        description = "Sacrifice Hope of Ghirapur: Until your next turn, target player who was dealt " +
            "combat damage by Hope of Ghirapur this turn can't cast noncreature spells."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Lius Lasahido"
        flavorText = "The renegades mounted an aether disruptor aboard a lightweight thopter to destroy " +
            "Tezzeret's Planar Bridge."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f4bcadd-7eff-4294-94d5-52482a734d5b.jpg?1783936726"
    }
}
