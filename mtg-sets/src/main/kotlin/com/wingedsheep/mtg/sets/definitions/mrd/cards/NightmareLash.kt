package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nightmare Lash — Mirrodin #219
 * {4} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1 for each Swamp you control.
 * Equip—Pay 3 life.
 *
 * The bonus is a Layer 7c dynamic bonus ([GrantDynamicStatsEffect]), recomputed at projection, so
 * a Swamp entering or leaving moves the equipped creature's stats immediately. "Swamp" is the land
 * *subtype*, not the card name — a Bad River or an animated dual counts, which is why the filter is
 * `Land.withSubtype(SWAMP)` rather than a name match. "You control" scopes to the Equipment's
 * controller, who may not be the equipped creature's controller.
 *
 * "Equip—Pay 3 life" is a non-mana equip cost, so it can't go through `equipAbility(...)` (that
 * helper only parses a mana cost). It is hand-rolled with `isEquipAbility = true` — which keeps the
 * sorcery-speed timing, the equip-cost rules, and the printed "Equip—Pay 3 life" menu rendering —
 * following Dark Knight's Greatsword. No `descriptionOverride`: there is no flavor name to preserve
 * and freezing the line would hide any cost-modifying effect.
 */
val NightmareLash = card("Nightmare Lash") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each Swamp you control.\n" +
        "Equip—Pay 3 life."

    staticAbility {
        val swamps = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Land.withSubtype(Subtype.SWAMP)
        ).count()
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.attachedCreature(),
            powerBonus = swamps,
            toughnessBonus = swamps
        )
    }

    activatedAbility {
        isEquipAbility = true
        cost = Costs.PayLife(3)
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Puddnhead"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f8e3fa6-494c-412f-90cc-36d45cd2b175.jpg?1783944510"
    }
}
