package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Time to Feed
 * {2}{G}
 * Sorcery
 * Choose target creature an opponent controls. When that creature dies this turn, you gain
 * 3 life. Target creature you control fights that creature.
 *
 * Composition: Devouring Tendrils' watched-target delayed `dies()` trigger on the first
 * target, then a fight. The delayed trigger is scheduled before the fight, so the creature
 * dying to the fight damage fires it; it also fires however else that creature dies this
 * turn. An illegal first target resolves to nothing, so no trigger is scheduled (no life);
 * an illegal second target leaves the trigger in place but skips the fight.
 */
val TimeToFeed = card("Time to Feed") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose target creature an opponent controls. When that creature dies this turn, you gain 3 life. " +
        "Target creature you control fights that creature. (Each deals damage equal to its power to the other.)"

    spell {
        val theirCreature = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        val yourCreature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        effect = Effects.CreateDelayedTrigger(
            effect = Effects.GainLife(3),
            trigger = Triggers.self.dies(),
            watchedTarget = theirCreature,
            expiry = DelayedTriggerExpiry.EndOfTurn,
        ) then Effects.Fight(yourCreature, theirCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/799d08a6-d978-4731-98ad-38acd57f3196.jpg?1783939736"
        ruling("2013-09-15", "Time to Feed has two targets: a creature an opponent controls and a creature you control. If only one of those creatures is a legal target when Time to Feed tries to resolve, the creatures won't fight and neither will deal or be dealt damage. If the creature you don't control is the illegal target, you won't gain life when it dies.")
        ruling("2013-09-15", "If neither creature is a legal target when Time to Feed tries to resolve, the spell won't resolve and none of its effects will happen.")
        ruling("2013-09-15", "If the first target creature dies that turn, you'll gain 3 life no matter what caused the creature to die or who controls the creature at that time.")
    }
}
