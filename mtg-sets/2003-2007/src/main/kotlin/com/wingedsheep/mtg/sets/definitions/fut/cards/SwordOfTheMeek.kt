package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sword of the Meek — Future Sight #165
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +1/+2.
 * Equip {2}
 * Whenever a 1/1 creature you control enters, you may return this card from your graveyard to the
 * battlefield, then attach it to that creature.
 *
 * - The last ability functions only from the graveyard (`triggerZone = GRAVEYARD`, Redtooth
 *   Vanguard's shape), so it triggers only if the Sword is there right after the creature enters
 *   (ruling). The 1/1 check reads the entering creature's projected power and toughness, so static
 *   abilities count and nothing cast in response can change whether it triggered (ruling).
 * - "Then attach it" is gated on the return actually happening (`IfYouDo`, inferred from the move
 *   growing the battlefield). The Sword keeps its id across the zone change, so the attach names the
 *   source. If the creature has left by then, the attach finds no host and the Sword stays on the
 *   battlefield unattached (ruling).
 */
val SwordOfTheMeek = card("Sword of the Meek") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+2.\n" +
        "Equip {2}\n" +
        "Whenever a 1/1 creature you control enters, you may return this card from your graveyard to " +
        "the battlefield, then attach it to that creature."

    staticAbility {
        ability = ModifyStats(1, 2)
    }

    equipAbility("{2}")

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.youControl()
                .power(1)
                .withCardPredicate(CardPredicate.ToughnessEquals(1))
        ).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
                then = Effects.AttachEquipment(EffectTarget.TriggeringEntity),
            )
        )
        description = "Whenever a 1/1 creature you control enters, you may return this card from your " +
            "graveyard to the battlefield, then attach it to that creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9f13705-6ede-4c29-a2b4-a082bf69e9c5.jpg?1783943091"
        ruling(
            "2020-08-07",
            "Sword of the Meek's last ability triggers only if it's in your graveyard immediately after " +
                "the 1/1 creature enters the battlefield."
        )
        ruling(
            "2020-08-07",
            "If a creature is entering the battlefield under your control, consider static abilities to " +
                "determine whether its power and toughness are both 1. Spells, activated abilities, and " +
                "triggered abilities can't be used to raise or lower the creature's power and toughness " +
                "in time to have the last ability trigger or to stop it from triggering."
        )
        ruling(
            "2020-08-07",
            "Once the last ability has triggered, changing the power or toughness of the creature won't " +
                "stop you from returning Sword of the Meek and attaching it to the creature."
        )
        ruling("2020-08-07", "Any creature can be equipped with Sword of the Meek, not just 1/1 creatures.")
        ruling(
            "2020-08-07",
            "If Sword of the Meek can't be attached to the creature that caused its last ability to " +
                "trigger, most likely because that creature has left the battlefield, Sword of the Meek " +
                "returns to the battlefield and remains unattached."
        )
    }
}
