package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Back for More
 * {4}{B}{G}
 * Instant
 * Return target creature card from your graveyard to the battlefield. When you do, it fights up to
 * one target creature you don't control.
 *
 * The fight is a reflexive triggered ability (CR 603.12): its target is chosen when it goes on the
 * stack, after the creature has returned, and players may respond to it. Curse of the Werefox's
 * wiring: the spell's own target is snapshotted into a pipeline collection before the reflexive
 * effect runs (the reflexive resolution replaces the context targets), so the fight reads the
 * returned creature from that collection and the opposing creature from the reflexive target.
 * `optional = true` on the reflexive requirement is the "up to one". Every creature you don't
 * control is one an opponent controls.
 */
val BackForMore = card("Back for More") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Return target creature card from your graveyard to the battlefield. When you do, it " +
        "fights up to one target creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Pipeline(
            descriptionOverride = "Return target creature card from your graveyard to the battlefield. " +
                "When you do, it fights up to one target creature you don't control."
        ) {
            val returned = gather(CardSource.ChosenTargets, name = "returnedCreature")
            run(
                Effects.ReflexiveTrigger(
                    action = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
                    optional = false,
                    descriptionOverride = "When you do, it fights up to one target creature you don't control."
                ) {
                    val opposingCreature = target(TargetFilter.CreatureOpponentControls, optional = true)
                    effect = Effects.Fight(returned.asTarget, opposingCreature)
                }
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Daarken"
        flavorText = "\"How can you be sure you pierced its heart? Do you even know where its heart is?\"\n—Trundan, Indatha poacher"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fc7210c-da23-4cec-9195-4de75587f40f.jpg?1783931029"
        ruling("2024-04-12", "You don't choose a target creature you don't control at the time you cast Back for More. Rather, a \"reflexive\" ability triggers when you return the target creature card from your graveyard to the battlefield. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal.")
        ruling("2020-04-17", "If the target creature is an illegal target when the reflexive triggered ability tries to resolve, the ability doesn't resolve. If the creature you put onto the battlefield is no longer on the battlefield or no longer a creature, the target creature won't deal or be dealt damage.")
    }
}
