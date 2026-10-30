package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Keensight Mentor
 * {2}{W}
 * Creature — Human Cleric
 * 1/4
 * When this creature enters, put a vigilance counter on target non-Human creature you control.
 * {1}{W}, {T}: Put a +1/+1 counter on each creature you control with vigilance.
 *
 * The activated ability is Dueling Coach's [Effects.ForEachInGroup] shape; "with vigilance" reads
 * projected keywords, so creatures holding a vigilance counter count.
 */
val KeensightMentor = card("Keensight Mentor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 4
    oracleText = "When this creature enters, put a vigilance counter on target non-Human creature you control.\n" +
        "{1}{W}, {T}: Put a +1/+1 counter on each creature you control with vigilance."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val recipient = target(TargetFilter(GameObjectFilter.Creature.youControl().notSubtype(Subtype.HUMAN)))
        effect = Effects.AddCounters(CounterType.VIGILANCE, 1, recipient)
        description = "When this creature enters, put a vigilance counter on target non-Human creature you control."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.VIGILANCE).youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "{1}{W}, {T}: Put a +1/+1 counter on each creature you control with vigilance."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Yongjae Choi"
        flavorText = "Through the *eludha*, bonders share the powerful abilities of their chosen companions."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0583065-9a6c-4ea4-b6dd-2edfbaed1181.jpg"
    }
}
