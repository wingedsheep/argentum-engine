package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Touch of Moonglove — Magic Origins #123
 * {B} · Instant
 * Target creature you control gets +1/+0 and gains deathtouch until end of turn. Whenever a
 * creature dealt damage by that creature dies this turn, its controller loses 2 life.
 *
 * The rider is an entity-scoped delayed trigger: [Triggers.self]'s `damagedCreatureDies()` with
 * `watchedTarget` set to the pumped creature, so it fires for each creature that creature damaged
 * this turn — read off the dying creature's last-known damage record, so a victim that dies in the
 * same combat as the pumped creature still counts. The dying creature is the triggering entity, so
 * "its controller" is that creature's controller, not the caster (2015-06-22 ruling).
 */
val TouchOfMoonglove = card("Touch of Moonglove") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 and gains deathtouch until end of turn. " +
        "Whenever a creature dealt damage by that creature dies this turn, its controller loses 2 life."

    spell {
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.ModifyStats(1, 0, creature) then
            Effects.GrantKeyword(Keyword.DEATHTOUCH, creature) then
            Effects.CreateDelayedTrigger(
                trigger = Triggers.self.damagedCreatureDies(),
                watchedTarget = creature,
                effect = Effects.LoseLife(2, EffectTarget.ControllerOfTriggeringEntity),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf659f2f-983c-4a62-a969-4176e5fbe193.jpg?1783938335"
        ruling("2015-06-22", "The controller of the creature dealt damage by the target creature is the one " +
            "who loses 2 life, not the controller of the target creature.")
    }
}
