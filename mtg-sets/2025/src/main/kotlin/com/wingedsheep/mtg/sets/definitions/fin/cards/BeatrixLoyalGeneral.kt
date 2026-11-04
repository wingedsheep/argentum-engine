package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Beatrix, Loyal General
 * {4}{W}{W}
 * Legendary Creature — Human Soldier
 * 4/4
 * Vigilance
 * At the beginning of combat on your turn, you may attach any number of Equipment you control
 * to target creature you control.
 *
 * The creature target is announced when the trigger goes on the stack. Consent and the choice
 * of Equipment happen on resolution. Equipment are not targeted; the pipeline attaches each
 * chosen Equipment to the locked creature target.
 *
 * Per the 2025-06-06 ruling, an Equipment that can't legally be attached to the target creature
 * (e.g. an Equipment that may only be attached to a specific kind of creature) can't be chosen;
 * the engine performs only legal attaches.
 */
val BeatrixLoyalGeneral = card("Beatrix, Loyal General") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "At the beginning of combat on your turn, you may attach any number of Equipment you " +
        "control to target creature you control."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.May(
            Effects.Pipeline {
                // Look at the Equipment you control; choose any number of them.
                val equipment = gather(
                    GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                    player = Player.You
                )
                val chosen = chooseAnyNumber(
                    from = equipment,
                    useTargetingUI = true,
                    prompt = "Choose any number of Equipment you control to attach to the target creature"
                )
                // Attach each chosen Equipment to the target creature.
                run(
                    Effects.ForEachInCollection(
                        chosen,
                        Effects.AttachTargetEquipmentToCreature(
                            equipmentTarget = EffectTarget.IterationEntity,
                            creatureTarget = creature
                        )
                    )
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "554"
        artist = "Bachzim"
        flavorText = "\"I commend your courage, but I will show you no mercy.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9da83b07-4978-4af7-be51-8aa8f35ec0bb.jpg?1782686128"

        ruling("2025-06-06", "You can't use Beatrix's last ability to try to attach an Equipment to a creature if that Equipment can't legally be attached to that creature.")
    }
}
