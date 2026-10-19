package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jugan Defends the Temple // Remnant of the Rising Star
 * {2}{G} — Enchantment — Saga // Enchantment Creature — Dragon Spirit 2/2
 *
 * Front — Jugan Defends the Temple:
 *   I — Create a 1/1 green Human Monk creature token with "{T}: Add {G}."
 *   II — Put a +1/+1 counter on each of up to two target creatures.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Remnant of the Rising Star:
 *   Flying
 *   Whenever another creature you control enters, you may pay {X}. When you do, put X +1/+1
 *   counters on that creature.
 *   As long as you control five or more modified creatures, this creature gets +5/+5 and has
 *   trample.
 *
 * "Modified" is [StatePredicate.IsModified] (CR 700.9); the Remnant counts itself when modified.
 */
private val FIVE_MODIFIED_CREATURES = Conditions.YouControlAtLeast(
    5,
    GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified),
)

private val RemnantOfTheRisingStar = card("Remnant of the Rising Star") {
    manaCost = ""
    colorIndicator = "G"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Dragon Spirit"
    oracleText = "Flying\n" +
        "Whenever another creature you control enters, you may pay {X}. When you do, put X +1/+1 " +
        "counters on that creature.\n" +
        "As long as you control five or more modified creatures, this creature gets +5/+5 and has " +
        "trample. (Equipment, Auras you control, and counters are modifications.)"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.MayPayX(
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                reflexiveEffect = Effects.AddDynamicCounters(
                    counterType = CounterType.PLUS_ONE_PLUS_ONE,
                    amount = DynamicAmounts.xValue(),
                    target = EffectTarget.TriggeringEntity,
                ),
                descriptionOverride = "put X +1/+1 counters on that creature",
            ),
        )
        description = "Whenever another creature you control enters, you may pay {X}. When you do, " +
            "put X +1/+1 counters on that creature."
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 5, toughnessBonus = 5, filter = GroupFilter.source()),
            condition = FIVE_MODIFIED_CREATURES,
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source()),
            condition = FIVE_MODIFIED_CREATURES,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "194"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/back/a/2/a22b5f27-f9e7-49e1-8e96-dc5d8e7fbe27.jpg?1783923851"
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}

private val JuganDefendsTheTempleFront = card("Jugan Defends the Temple") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Create a 1/1 green Human Monk creature token with \"{T}: Add {G}.\"\n" +
        "II — Put a +1/+1 counter on each of up to two target creatures.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Human", "Monk"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.Tap,
                    effect = Effects.AddMana(Color.GREEN),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "{T}: Add {G}."
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/8/6/86dd086e-c757-4a6a-8838-51860f2095f8.jpg?1783923713"
        )
    }

    sagaChapter(2) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "194"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a22b5f27-f9e7-49e1-8e96-dc5d8e7fbe27.jpg?1783923851"
    }
}

val JuganDefendsTheTemple: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = JuganDefendsTheTempleFront,
    backFace = RemnantOfTheRisingStar,
)
