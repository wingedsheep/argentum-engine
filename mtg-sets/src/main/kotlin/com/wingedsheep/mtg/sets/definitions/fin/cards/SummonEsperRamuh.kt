package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Summon: Esper Ramuh
 * {2}{R}{R}
 * Enchantment Creature — Saga Wizard
 * 3/3
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Judgment Bolt — This creature deals damage equal to the number of noncreature, nonland
 *   cards in your graveyard to target creature an opponent controls.
 * II, III — Wizards you control get +1/+0 until end of turn.
 *
 * Chapter I deals damage sourced by the saga-creature itself (EffectTarget.Self) equal to a
 * graveyard count — noncreature AND nonland cards in the controller's graveyard — to a single
 * mandatory target creature an opponent controls. Chapters II and III share one team pump:
 * Wizards you control (including the saga, itself a Wizard) get +1/+0 until end of turn.
 */
val SummonEsperRamuh = card("Summon: Esper Ramuh") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Saga Wizard"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Judgment Bolt — This creature deals damage equal to the number of noncreature, nonland cards in your graveyard to target creature an opponent controls.\n" +
        "II, III — Wizards you control get +1/+0 until end of turn."
    power = 3
    toughness = 3

    // Chapter I — "Judgment Bolt": damage = noncreature, nonland cards in your graveyard, dealt by
    // this creature to a target creature an opponent controls.
    sagaChapter(1) {
        val victim = target("creature", TargetObject(filter = TargetFilter.CreatureOpponentControls))
        val noncreatureNonland = DynamicAmounts.zone(
            Player.You,
            Zone.GRAVEYARD,
            GameObjectFilter.Nonland.notCreature(),
        ).count()
        effect = Effects.DealDamage(noncreatureNonland, victim, damageSource = EffectTarget.Self)
    }

    // Chapters II & III — "Wizards you control get +1/+0 until end of turn."
    val wizardPump = Patterns.Group.modifyStatsForAll(
        power = 1,
        toughness = 0,
        filter = GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Wizard")),
    )
    sagaChapter(2) { effect = wizardPump }
    sagaChapter(3) { effect = wizardPump }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Justyna Dura"
        flavorText = "\"I am Ramuh—the esper.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/4/840659ee-1493-4190-a514-c2c9ae14e331.jpg?1748706366"
    }
}
