package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Behemoth of Vault 0
 * {6}
 * Artifact Creature — Robot
 * 6/6
 *
 * Trample
 * When this creature enters, you get {E}{E}{E}{E} (four energy counters).
 * When this creature dies, you may pay an amount of {E} equal to target nonland permanent's mana
 * value. When you do, destroy that permanent.
 *
 * The dies trigger targets as it goes on the stack (ruling); the energy payment is decided on
 * resolution — an all-or-nothing [Effects.PayExactCounters] over the target's mana value, only
 * offered when you have enough. Paying fires an untargeted reflexive trigger (CR 603.12) that
 * destroys "that permanent": the outer target is carried into it through a pipeline collection
 * (the Curse of the Werefox shape), since the reflexive ability chooses no targets of its own.
 */
val BehemothOfVault0 = card("Behemoth of Vault 0") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Robot"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "When this creature enters, you get {E}{E}{E}{E} (four energy counters).\n" +
        "When this creature dies, you may pay an amount of {E} equal to target nonland permanent's " +
        "mana value. When you do, destroy that permanent."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(4)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Pipeline(
            descriptionOverride = "You may pay an amount of {E} equal to target nonland permanent's " +
                "mana value. When you do, destroy that permanent."
        ) {
            val that = gather(CardSource.ChosenTargets, name = "behemothTarget")
            run(
                Effects.ReflexiveTrigger(
                    action = Effects.PayExactCounters(CounterType.ENERGY, DynamicAmounts.manaValueOf(permanent)),
                    reflexiveEffect = Effects.Destroy(that.asTarget),
                    optional = true,
                    descriptionOverride = "When you do, destroy that permanent."
                )
            )
        }
        description = "When this creature dies, you may pay an amount of {E} equal to target nonland " +
            "permanent's mana value. When you do, destroy that permanent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Eddie Mendoza"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/592d6e5b-a94e-460c-ab2f-21fd842c3d9a.jpg?1783912381"
        ruling(
            "2024-03-08",
            "You choose the target of Behemoth of Vault 0's last ability when it triggers, but you don't " +
                "have to decide whether or not to pay until the ability resolves."
        )
        ruling(
            "2024-03-08",
            "When you pay {E} equal to the target nonland permanent's mana value during the resolution of " +
                "Behemoth of Vault 0's last ability, a second \"reflexive\" ability triggers and is put onto " +
                "the stack. This ability does not have a target. Each player may respond to this triggered " +
                "ability as normal."
        )
    }
}
