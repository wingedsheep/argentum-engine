package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * T-45 Power Armor (PIP #145) — {2} Artifact — Equipment
 *
 * When this Equipment enters, you get {E}{E} (two energy counters).
 * Equipped creature gets +3/+3 and doesn't untap during its controller's untap step.
 * At the beginning of your upkeep, you may pay {E}. If you do, untap equipped creature, then put
 * your choice of a menace, trample, or lifelink counter on it.
 * Equip {3}
 *
 * The untap lock is Vulshok Gauntlets' shape: an [AbilityFlag.DOESNT_UNTAP] grant over
 * [Filters.EquippedCreature], separate from the +3/+3 (different layers). "You may pay {E}. If you
 * do" is an [Effects.MayPay] over an exact one-energy payment (Assaultron Dominator's shape); the
 * counter kind is chosen at resolution, after paying, via [Effects.ChooseAction]. "Equipped
 * creature" / "it" is read at resolution through [EffectTarget.EquippedCreature] — with nothing
 * equipped you may still pay, and the untap and counter simply do nothing.
 */
val T45PowerArmor = card("T-45 Power Armor") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, you get {E}{E} (two energy counters).\n" +
        "Equipped creature gets +3/+3 and doesn't untap during its controller's untap step.\n" +
        "At the beginning of your upkeep, you may pay {E}. If you do, untap equipped creature, then " +
        "put your choice of a menace, trample, or lifelink counter on it.\n" +
        "Equip {3}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    staticAbility {
        ability = ModifyStats(+3, +3, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 1),
            then = Effects.Untap(EffectTarget.EquippedCreature) then Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        label = "Menace counter",
                        effect = Effects.AddCounters(CounterType.MENACE, 1, EffectTarget.EquippedCreature),
                    ),
                    EffectChoice(
                        label = "Trample counter",
                        effect = Effects.AddCounters(CounterType.TRAMPLE, 1, EffectTarget.EquippedCreature),
                    ),
                    EffectChoice(
                        label = "Lifelink counter",
                        effect = Effects.AddCounters(CounterType.LIFELINK, 1, EffectTarget.EquippedCreature),
                    ),
                )
            ),
        )
        description = "At the beginning of your upkeep, you may pay {E}. If you do, untap equipped " +
            "creature, then put your choice of a menace, trample, or lifelink counter on it."
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/203b1cb3-5eae-469a-8a5a-2e56dc859f78.jpg?1783912374"
        ruling("2024-03-08", "Multiple instances of menace are redundant. The same is true for trample and lifelink.")
    }
}
