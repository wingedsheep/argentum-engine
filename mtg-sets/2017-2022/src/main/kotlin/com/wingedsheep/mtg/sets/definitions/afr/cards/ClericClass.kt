package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cleric Class {W}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * If you would gain life, you gain that much life plus 1 instead.
 *
 * {3}{W}: Level 2
 * Whenever you gain life, put a +1/+1 counter on target creature you control.
 *
 * {4}{W}: Level 3
 * When this Class becomes level 3, return target creature card from your graveyard to the
 * battlefield. You gain life equal to that creature's toughness.
 *
 * Level 1 is Heron of Hope's `ModifyLifeGain` (+1) replacement; level 2 is a plain life-gain
 * trigger, once per life-gain event; level 3 is the guarded graveyard return followed by a life
 * gain that reads the returned creature's toughness live, so it uses its toughness as it exists on
 * the battlefield (ruling 2021-07-23) — and level 1's replacement adds 1 to that gain.
 */
val ClericClass = card("Cleric Class") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "If you would gain life, you gain that much life plus 1 instead.\n" +
        "{3}{W}: Level 2 — Whenever you gain life, put a +1/+1 counter on target creature you control.\n" +
        "{4}{W}: Level 3 — When this Class becomes level 3, return target creature card from your " +
        "graveyard to the battlefield. You gain life equal to that creature's toughness."

    // Level 1: if you would gain life, you gain that much plus 1 instead.
    replacementEffect(
        ModifyLifeGain(
            multiplier = 1,
            modifier = 1,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You),
        )
    )

    // Level 2: whenever you gain life, put a +1/+1 counter on target creature you control.
    classLevel(2, "{3}{W}") {
        triggeredAbility {
            trigger = Triggers.you.gainsLife()
            val creature = target(TargetFilter.Creature.youControl())
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            description = "Whenever you gain life, put a +1/+1 counter on target creature you control."
        }
    }

    // Level 3: reanimate target creature card, then gain life equal to its battlefield toughness.
    classLevel(3, "{4}{W}") {
        triggeredAbility {
            trigger = Triggers.self.enters()
            val creature = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(creature) then
                Effects.GainLife(DynamicAmounts.toughnessOf(creature))
            description = "When this Class becomes level 3, return target creature card from your " +
                "graveyard to the battlefield. You gain life equal to that creature's toughness."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47ce8b7e-d8e1-489a-a69e-99089eeb8739.jpg?1783926538"
        ruling("2021-07-23", "If you gain life at the same time a creature is dealt lethal damage, it dies before Cleric Class's level 2 ability can put a +1/+1 counter on it.")
        ruling("2021-07-23", "An ability that triggers \"whenever you gain life\" triggers just once for each life gain event, no matter how much life you gain.")
        ruling("2021-07-23", "Each creature with lifelink dealing combat damage causes a separate life gain event. For example, if two creatures you control with lifelink deal combat damage at the same time, you will gain 2 additional life from Cleric Class's first ability and its level 2 ability will trigger twice. However, if a single creature you control with lifelink deals combat damage to multiple creatures, players, and/or planeswalkers at the same time (perhaps because it has trample or was blocked by more than one creature), you will only gain 1 additional life and the ability will trigger only once.")
        ruling("2021-07-23", "If you gain an amount of life \"for each\" of something, that life is gained as one event and the ability will trigger only once.")
        ruling("2021-07-23", "When Cleric Class's level 3 ability resolves, you gain life equal to the creature's toughness as it exists on battlefield, which may be different than its toughness in the graveyard.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2021-07-23", "You can't activate the first level ability of a Class unless that Class is level 1. Similarly, you can't activate the second level ability of a Class unless that Class is level 2.")
    }
}
