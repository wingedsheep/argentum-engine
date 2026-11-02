package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kira Nerys, Firebrand
 * {1}{R}{W}
 * Legendary Creature — Bajoran Soldier
 * 4/3
 *
 * Trample
 * If a creature you control would enter while you control an attacking creature, instead it enters
 * tapped and attacking.
 * Whenever another creature you control enters, it gets +1/+1 until end of turn.
 *
 * The replacement is a global [PermanentsEnterTapped] with `attackDefender = CONTROLLER_CHOOSES`
 * (CR 508.4: the entering creature's controller picks what it attacks), gated on Kira's controller
 * controlling an attacking creature. A creature entering this way is attacking but never "attacked".
 */
val KiraNerysFirebrand = card("Kira Nerys, Firebrand") {
    manaCost = "{1}{R}{W}"
    typeLine = "Legendary Creature — Bajoran Soldier"
    power = 4
    toughness = 3
    oracleText = "Trample\n" +
        "If a creature you control would enter while you control an attacking creature, instead it enters tapped and attacking.\n" +
        "Whenever another creature you control enters, it gets +1/+1 until end of turn."

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD,
            ),
            condition = Conditions.YouControl(GameObjectFilter.Creature.attacking()),
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
        )
    )

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.ModifyStats(1, 1, EffectTarget.TriggeringEntity)
        description = "Whenever another creature you control enters, it gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Pauline Voss"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81ad6427-e730-47c9-8310-6d3ca4b10c82.jpg?1793013295"
    }
}
