package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invasion of New Capenna // Holy Frazzle-Cannon — March of the Machine #238.
 * {W}{B} · Battle — Siege · defense 4 // Artifact — Equipment
 *
 * Front: an optional sacrifice of an artifact or creature feeds a reflexive trigger that exiles
 * target artifact or creature an opponent controls — the target is chosen only once the
 * sacrifice happened (per the ruling).
 *
 * Back: whenever the equipped creature attacks, it gets a +1/+1 counter, and so does each *other*
 * creature you control that shares a creature type with it. The attacker is counted once through
 * the first leg; the group excludes it so no creature gets two counters (per the ruling), and an
 * attacker with no creature types still gets its own counter.
 */
private val InvasionOfNewCapennaFront = card("Invasion of New Capenna") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, you may sacrifice an artifact or creature. When you do, exile " +
        "target artifact or creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.SacrificeOwn(filter = GameObjectFilter.CreatureOrArtifact),
            optional = true,
        ) {
            val victim = target(TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls()))
            effect = Effects.Exile(victim)
        }
        description = "When this Siege enters, you may sacrifice an artifact or creature. When you " +
            "do, exile target artifact or creature an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5926c8e-8049-418e-9a84-f8558e5ba9d1.jpg?1783916949"
        ruling("2023-04-14", "Invasion of New Capenna's ability triggers and goes on the stack without a target. If you sacrifice an artifact or creature, the second \"reflexive\" triggered ability will trigger. You'll choose the target artifact or creature for that second ability at that time.")
    }
}

private val HolyFrazzleCannon = card("Holy Frazzle-Cannon") {
    manaCost = ""
    colorIdentity = "WB"
    colorIndicator = "WB"
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature attacks, put a +1/+1 counter on that creature and " +
        "each other creature you control that shares a creature type with it.\n" +
        "Equip {1}"

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity) then
            Effects.ForEachInGroup(
                GroupFilter(
                    GameObjectFilter.Creature.youControl()
                        .sharingCreatureTypeWith(EffectTarget.TriggeringEntity)
                ).otherThanTriggeringEntity(),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
            )
        description = "Whenever equipped creature attacks, put a +1/+1 counter on that creature " +
            "and each other creature you control that shares a creature type with it."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Diego Gisbert"
        flavorText = "\"They're using my Halo stockpile for what?\"\n—Jetmir"
        imageUri = "https://cards.scryfall.io/normal/back/f/5/f5926c8e-8049-418e-9a84-f8558e5ba9d1.jpg?1783916949"
        ruling("2023-04-14", "A creature \"shares a creature type\" with the equipped creature if they have at least one creature type in common. Any one creature will get only one +1/+1 counter this way, even if it has multiple creature types in common with the equipped creature.")
    }
}

val InvasionOfNewCapenna: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfNewCapennaFront,
    backFace = HolyFrazzleCannon,
)
