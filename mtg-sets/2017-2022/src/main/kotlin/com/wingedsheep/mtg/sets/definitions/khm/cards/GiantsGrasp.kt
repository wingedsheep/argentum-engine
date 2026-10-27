package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Giant's Grasp
 * {2}{U}{U}
 * Enchantment — Aura
 * Enchant Giant you control
 * When this Aura enters, gain control of target nonland permanent for as long as this Aura
 * remains on the battlefield.
 *
 * Same shape as Sower of Temptation: the grab belongs to the resolving enters trigger and rides
 * [Duration.WhileSourceOnBattlefield] keyed to the Aura, so if the Aura leaves before the trigger
 * resolves, nothing is stolen, and the permanent returns the moment the Aura leaves.
 */
val GiantsGrasp = card("Giant's Grasp") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant Giant you control\n" +
        "When this Aura enters, gain control of target nonland permanent for as long as this Aura " +
        "remains on the battlefield."

    auraTarget = TargetObject(
        filter = TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.GIANT).youControl())
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.GainControl(
            permanent,
            Duration.WhileSourceOnBattlefield("this Aura")
        )
        description = "gain control of target nonland permanent for as long as this Aura remains on the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "384"
        artist = "Ilse Gort"
        flavorText = "\"Behave and you'll be a fine pet. Bite me and you're ammunition.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74c92d48-504a-4320-b9f4-c1d2d06fa223.jpg?1783928120"

        ruling(
            "2021-02-05",
            "If Giant's Grasp leaves the battlefield before its triggered ability resolves, you won't gain " +
                "control of the target nonland permanent at all."
        )
        ruling(
            "2021-02-05",
            "The control-changing effect doesn't expire if another player gains control of Giant's Grasp " +
                "(although this is likely to cause it to leave the battlefield because it can enchant only a " +
                "Giant controlled by the controller of Giant's Grasp). The effect expires only once Giant's " +
                "Grasp is no longer on the battlefield."
        )
        ruling(
            "2021-02-05",
            "Gaining control of a nonland permanent doesn't cause you to gain control of any Auras or " +
                "Equipment attached to it."
        )
    }
}
