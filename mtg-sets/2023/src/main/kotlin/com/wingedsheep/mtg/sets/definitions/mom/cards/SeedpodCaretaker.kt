package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Seedpod Caretaker — March of the Machine #325
 * {2}{W} · Creature — Phyrexian Cleric · 2/2
 *
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on target artifact or creature you control.
 * • Transform target Incubator token you control.
 *
 * A modal enters trigger (like Etched Host Doombringer); the transform mode's Incubator is
 * *targeted*, the same filter Progenitor Exarch's {T} ability uses.
 */
val SeedpodCaretaker = card("Seedpod Caretaker") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Cleric"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on target artifact or creature you control.\n" +
        "• Transform target Incubator token you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Put a +1/+1 counter on target artifact or creature you control.") {
                val permanent = target(TargetFilter.CreatureOrArtifact.youControl())
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, permanent)
            },
            mode("Transform target Incubator token you control.") {
                val incubator = target(
                    TargetFilter(GameObjectFilter.Permanent.withSubtype("Incubator").token().youControl())
                )
                effect = Effects.Transform(incubator)
            }
        )
        description = "When this creature enters, choose one — Put a +1/+1 counter on target artifact or " +
            "creature you control; or transform target Incubator token you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "325"
        artist = "Slawomir Maniak"
        flavorText = "His body clicked and hummed—a lullaby meant only for machines."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0861bda4-d014-4908-aed5-3f3bf78704cf.jpg?1783916906"
    }
}
