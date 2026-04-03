package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Blacksmith's Talent {R}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 *
 * When this Class enters, create a colorless Equipment artifact token named Sword
 * with "Equipped creature gets +1/+1" and equip {2}.
 *
 * {2}{R}: Level 2
 * At the beginning of combat on your turn, attach target Equipment you control to
 * up to one target creature you control.
 *
 * {3}{R}: Level 3
 * During your turn, equipped creatures you control have double strike and haste.
 */
val BlacksmithsTalent = card("Blacksmith's Talent") {
    manaCost = "{R}"
    typeLine = "Enchantment — Class"
    oracleText = "When this Class enters, create a colorless Equipment artifact token named Sword with \"Equipped creature gets +1/+1\" and equip {2}.\n" +
        "{2}{R}: Level 2 — At the beginning of combat on your turn, attach target Equipment you control to up to one target creature you control.\n" +
        "{3}{R}: Level 3 — During your turn, equipped creatures you control have double strike and haste."

    // Level 1: ETB — create a Sword Equipment token
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateSword()
    }

    // Level 2: At the beginning of combat on your turn, attach target Equipment to up to one target creature
    classLevel(2, "{2}{R}") {
        triggeredAbility {
            trigger = Triggers.BeginCombat
            val equipment = target(
                "Equipment you control",
                TargetPermanent(
                    filter = TargetFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl())
                )
            )
            val creature = target(
                "creature you control",
                TargetPermanent(
                    filter = TargetFilter(GameObjectFilter.Creature.youControl()),
                    optional = true
                )
            )
            effect = Effects.AttachTargetEquipmentToCreature(equipment, creature)
        }
    }

    // Level 3: During your turn, equipped creatures you control have double strike and haste
    classLevel(3, "{3}{R}") {
        staticAbility {
            condition = Conditions.IsYourTurn
            ability = GrantKeywordToCreatureGroup(
                keyword = Keyword.DOUBLE_STRIKE,
                filter = GroupFilter(GameObjectFilter.Creature.youControl().equipped())
            )
        }
        staticAbility {
            condition = Conditions.IsYourTurn
            ability = GrantKeywordToCreatureGroup(
                keyword = Keyword.HASTE,
                filter = GroupFilter(GameObjectFilter.Creature.youControl().equipped())
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Vincent Christiaens"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bb318fa-481d-40a7-978e-f01b49101ae0.jpg?1739659737"
        ruling("2024-07-26", "If either target of the Level 2 class ability is an illegal target as the ability resolves, the ability won't do anything. If both targets are illegal, the ability won't resolve. If the Equipment is already attached to the target creature, nothing happens.")
        ruling("2024-07-26", "A creature you control is equipped if there's an Equipment attached to it. You don't have to control that Equipment.")
    }
}
