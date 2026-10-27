package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kaya's Ghostform — War of the Spark #94 (canonical printing)
 * {B} · Enchantment — Aura
 *
 * Enchant creature or planeswalker you control
 * When enchanted permanent dies or is put into exile, return that card to the battlefield under
 * your control.
 *
 * "Dies or is put into exile" is two attached-binding triggers, one per destination (the Kaya the
 * Inexorable split), each returning the card *from that zone*: the `fromZone` guard leaves a card
 * that has already moved on (or a token, which ceased to exist) where it is, per the rulings. The
 * attached binding looks back at the Aura's last attachment, so the trigger still fires when the
 * Aura goes to the graveyard alongside the permanent.
 */
val KayasGhostform = card("Kaya's Ghostform") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or planeswalker you control\n" +
        "When enchanted permanent dies or is put into exile, return that card to the battlefield under your control."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.youControl()))

    val returnText = "When enchanted permanent dies or is put into exile, return that card to the " +
        "battlefield under your control."

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.TriggeringEntity, underYourControl = true)
        description = returnText
    }

    triggeredAbility {
        trigger = Triggers.attached.leaves(to = Zone.EXILE)
        effect = Effects.Move(
            EffectTarget.TriggeringEntity,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.EXILE,
        )
        description = returnText
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Johan Grenier"
        flavorText = "The Eternals found some sparks to be quite slippery."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98f3be88-24e2-433f-9138-f0cb59d09a0f.jpg?1783933444"

        ruling("2019-05-03", "Kaya's Ghostform can enchant a token, but its last ability won't return the token to the battlefield.")
        ruling(
            "2019-05-03",
            "If an effect exiles the enchanted permanent and immediately returns it to the battlefield, the last " +
                "ability of Kaya's Ghostform triggers but will have no effect. If an effect exiles the enchanted " +
                "permanent and would return it to the battlefield at a later time, Kaya's Ghostform will return " +
                "that card to the battlefield and it won't be returned later."
        )
        ruling(
            "2019-05-03",
            "If the enchanted permanent is put into a graveyard or exile but leaves that zone before the last " +
                "ability of Kaya's Ghostform resolves, that card stays in its new zone, even if that zone is also " +
                "a graveyard or exile. You don't return it to the battlefield."
        )
        ruling(
            "2019-05-03",
            "If Kaya's Ghostform and the enchanted permanent are both put into graveyards and/or exiled at the " +
                "same time, the enchanted permanent will be returned to the battlefield."
        )
    }
}
