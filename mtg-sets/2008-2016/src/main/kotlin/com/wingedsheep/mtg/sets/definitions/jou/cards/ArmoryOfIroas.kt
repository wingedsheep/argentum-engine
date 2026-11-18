package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Armory of Iroas — Journey into Nyx #158 (canonical printing)
 * {2} · Artifact — Equipment
 *
 * Whenever equipped creature attacks, put a +1/+1 counter on it.
 * Equip {2}
 *
 * The Equipment's own attack trigger binds the attacking creature as
 * [EffectTarget.TriggeringEntity], so "it" is the creature that attacked even if the Armory is
 * moved to another creature in response.
 */
val ArmoryOfIroas = card("Armory of Iroas") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature attacks, put a +1/+1 counter on it.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    // Whenever equipped creature attacks, put a +1/+1 counter on it.
    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        description = "Whenever equipped creature attacks, put a +1/+1 counter on it."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Yeong-Hao Han"
        flavorText = "From a distance the weapons look pristine, but in fact they are tarnished and dented, " +
            "each as unique as the champion who carried it to battle."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7ac44e4b-25ba-4b32-ad56-c87c202c310a.jpg?1783939400"
        ruling(
            "2020-11-10",
            "If you move Armory of Iroas from one creature to another, any +1/+1 counters on the first " +
                "creature remain where they are."
        )
    }
}
