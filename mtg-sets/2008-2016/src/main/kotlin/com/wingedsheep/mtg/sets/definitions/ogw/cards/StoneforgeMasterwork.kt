package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stoneforge Masterwork — Oath of the Gatewatch #166
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1 for each other creature you control that shares a creature type
 * with it.
 * Equip {2}
 *
 * Modelling notes:
 * - "it" is the equipped creature: the filter compares against [EffectTarget.AffectedEntity] (the
 *   creature the bonus is being applied to), and `excludeSelf` drops that same affected creature,
 *   so the equipped creature never counts itself (the Alpha Status composition).
 * - "you control" is the *Equipment controller*: [Player.You] resolves against the ability's
 *   source, the Masterwork, which matters once the equipped creature changes controller.
 * - Types are read from projected state, so changelings and granted creature types count.
 */
val StoneforgeMasterwork = card("Stoneforge Masterwork") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each other creature you control that shares a creature type with it.\nEquip {2}"

    staticAbility {
        val sharedTypeCount = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Creature.sharingCreatureTypeWith(EffectTarget.AffectedEntity),
            excludeSelf = true,
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = sharedTypeCount,
            toughnessBonus = sharedTypeCount,
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Ben Maier"
        flavorText = "The nomadic kor carry only necessities on their long treks. The coming of the Eldrazi redefined \"necessities.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae5b78f5-6a49-4433-a42b-5abf5ea60919.jpg?1783937894"

        ruling("2016-01-22", "The bonus is only +1/+1 per applicable creature, even if it shares more than one creature type with the equipped creature. For example, if the equipped creature is a Human Shaman and you control a Human Warrior, a Goblin Shaman, and another Human Shaman, the equipped creature will get +3/+3.")
        ruling("2016-01-22", "The bonus will change as the creatures you control change. This may cause the equipped creature to lose toughness and die because of damage dealt earlier in the turn.")
    }
}
