package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gruul Ragebeast
 * {5}{R}{G}
 * Creature — Beast
 * 6/6
 * Whenever this creature or another creature you control enters, that creature fights target
 * creature an opponent controls.
 *
 * "This creature or another creature you control" is `Triggers.a(...)` (self included), and the
 * fighter is the entering creature — [EffectTarget.TriggeringEntity] — not the Ragebeast. The
 * trigger is mandatory; with no creature an opponent controls it has no legal target and is
 * removed from the stack. If either fighter is gone on resolution, no damage is dealt.
 */
val GruulRagebeast = card("Gruul Ragebeast") {
    manaCost = "{5}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 6
    oracleText = "Whenever this creature or another creature you control enters, that creature " +
        "fights target creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        val theirs = target(TargetFilter.Creature.opponentControls())
        effect = Effects.Fight(EffectTarget.TriggeringEntity, theirs)
        description = "Whenever this creature or another creature you control enters, that creature " +
            "fights target creature an opponent controls."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Dave Kendall"
        flavorText = "\"If the other guilds want a war, they should know we've been ready the whole " +
            "time.\"\n—Ruric Thar, Ghor-Clan chieftain"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/080ef367-7904-4e5c-a8b4-1fb62f951f3e.jpg?1783940106"
        ruling(
            "2013-01-24",
            "If the target creature is an illegal target when the ability tries to resolve, it won't " +
                "resolve and none of its effects will happen. Neither creature will deal or be dealt damage."
        )
        ruling(
            "2013-01-24",
            "Gruul Ragebeast's triggered ability isn't optional. However, if no opponent controls a " +
                "creature when a creature enters under your control, the ability will be removed from the stack."
        )
        ruling(
            "2013-01-24",
            "If one or both of the creatures that are supposed to fight are no longer on the battlefield " +
                "when the ability resolves, the fight won't happen. Neither creature will deal or be dealt damage."
        )
    }
}
