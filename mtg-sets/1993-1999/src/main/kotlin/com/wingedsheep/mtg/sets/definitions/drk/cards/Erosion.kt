package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Erosion
 * {U}{U}{U}
 * Enchantment — Aura
 * Enchant land
 * At the beginning of the upkeep of enchanted land's controller, destroy that land unless that
 * player pays {1} or 1 life.
 *
 * Lingering Death's trigger shape — an ATTACHED-bound step trigger, timed off the *enchanted
 * permanent's* controller, who is bound as the triggering player. The Aura's controller still
 * controls the ability, so the payer is named `TriggeringPlayer`: the tax falls on whoever
 * controls the land, not on whoever cast the Aura.
 *
 * "{1} **or** 1 life" is a genuine choice of costs, so it is `Costs.pay.Choice` rather than two
 * effects or a life-only shortcut; a player with no mana can still pay, and one at 1 life can still
 * pay the {1}.
 *
 * The land is destroyed, not sacrificed, so regeneration and indestructibility apply.
 */
val Erosion = card("Erosion") {
    manaCost = "{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "At the beginning of the upkeep of enchanted land's controller, destroy that land unless " +
        "that player pays {1} or 1 life."
    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Choice(listOf(Costs.pay.Mana("{1}"), Costs.pay.PayLife(1))),
            suffer = Effects.Destroy(EffectTarget.EnchantedPermanent),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "At the beginning of the upkeep of enchanted land's controller, destroy " +
            "that land unless that player pays {1} or 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f4b6507-89ee-482e-aafd-8e05ada8f1ce.jpg?1783947945"
    }
}
