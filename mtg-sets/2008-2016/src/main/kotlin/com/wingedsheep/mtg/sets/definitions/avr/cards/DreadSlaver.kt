package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dread Slaver — Avacyn Restored #98 (earliest printing; reprinted in Jumpstart 2022)
 * {3}{B}{B}
 * Creature — Zombie Horror
 * 3/5
 * Whenever a creature dealt damage by this creature this turn dies, return it to the battlefield
 * under your control. That creature is a black Zombie in addition to its other colors and types.
 *
 * Still triggers when Dread Slaver dies alongside the creature it damaged (CR 603.10a look-back).
 */
val DreadSlaver = card("Dread Slaver") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Horror"
    power = 3
    toughness = 5
    oracleText = "Whenever a creature dealt damage by this creature this turn dies, return it to the battlefield " +
        "under your control. That creature is a black Zombie in addition to its other colors and types."

    triggeredAbility {
        trigger = Triggers.self.damagedCreatureDies()
        // Only a card still in the graveyard returns, and only a returned creature becomes a Zombie
        // (ruling 2012-05-01: if undying already brought it back, nothing happens).
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.TriggeringEntity,
                GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD)
            ),
            then = Effects.PutOntoBattlefieldUnderYourControl(EffectTarget.TriggeringEntity) then
                Effects.AddColor(Color.BLACK, EffectTarget.TriggeringEntity, Duration.Permanent) then
                Effects.AddCreatureType("Zombie", EffectTarget.TriggeringEntity, Duration.Permanent)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Dave Kendall"
        flavorText = "Half a brain rules the mindless."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d8a3abd-a4a2-48e6-b709-1c0240a76c5e.jpg?1783940701"
        ruling("2012-05-01", "Each time a creature dies, check whether Dread Slaver had dealt any damage to it at any time during that turn. If so, Dread Slaver's ability will trigger. It doesn't matter who controlled the creature or whose graveyard it was put into.")
        ruling("2012-05-01", "If Dread Slaver and a creature it was blocking or blocked by both die in combat, Dread Slaver's ability will trigger.")
        ruling("2012-05-01", "Dread Slaver will return if it somehow dealt damage to itself and then dies.")
        ruling("2012-05-01", "The card will return to the battlefield under your control only if it's still in the graveyard when the ability resolves. If it's not (perhaps because an ability like undying has already returned it to the battlefield), nothing happens.")
    }
}
