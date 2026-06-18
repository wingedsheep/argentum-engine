package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ringwraiths
 * {4}{B}{B}
 * Creature — Wraith Knight
 * 5/5
 *
 * When this creature enters, target creature an opponent controls gets -3/-3 until end of turn. If
 * that creature is legendary, its controller loses 3 life.
 * When the Ring tempts you, return this card from your graveyard to your hand.
 *
 * Gap 21 (graveyard-functional triggered ability) is engine-landed (`TriggeredAbility.activeZone`,
 * DSL `triggerZone = Zone.GRAVEYARD`; templates Pyre Zombie, Persistent Marshstalker). The legendary
 * rider is checked mid-resolution (the targeted creature is still on the battlefield until SBAs run
 * after the ability finishes), so printed order works.
 */
val Ringwraiths = card("Ringwraiths") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wraith Knight"
    power = 5
    toughness = 5
    oracleText = "When this creature enters, target creature an opponent controls gets -3/-3 until end " +
        "of turn. If that creature is legendary, its controller loses 3 life.\n" +
        "When the Ring tempts you, return this card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.ModifyStats(-3, -3, creature).then(
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.legendary(), 0),
                effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.ControllerOf("target creature")))
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.RingTemptsYou
        triggerZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "284"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a8495c3-96cf-40ab-b68a-1b5711b7659e.jpg?1719684265"
    }
}
