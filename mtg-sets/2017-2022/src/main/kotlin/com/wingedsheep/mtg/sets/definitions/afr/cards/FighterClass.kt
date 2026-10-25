package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceEquipCost
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fighter Class {R}{W}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * When this Class enters, search your library for an Equipment card, reveal it, put it into your
 * hand, then shuffle.
 *
 * {1}{R}{W}: Level 2
 * Equip abilities you activate cost {2} less to activate.
 *
 * {3}{R}{W}: Level 3
 * Whenever a creature you control attacks, up to one target creature blocks it this combat if able.
 *
 * Level 1 is Cloud, Midgar Mercenary's revealed Equipment tutor; level 2 is Éowyn, Lady of Rohan's
 * controller-scoped [ReduceEquipCost] (the equip-reduction scan reads the Class's reached level);
 * level 3 is Tolsimir's ANY-bound attack trigger pinning the blocker to the triggering attacker.
 */
val FighterClass = card("Fighter Class") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "When this Class enters, search your library for an Equipment card, reveal it, put it into " +
        "your hand, then shuffle.\n" +
        "{1}{R}{W}: Level 2 — Equip abilities you activate cost {2} less to activate.\n" +
        "{3}{R}{W}: Level 3 — Whenever a creature you control attacks, up to one target creature " +
        "blocks it this combat if able."

    // Level 1: tutor an Equipment on entering.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.withSubtype("Equipment"),
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        )
        description = "When this Class enters, search your library for an Equipment card, reveal it, " +
            "put it into your hand, then shuffle."
    }

    // Level 2: equip abilities you activate cost {2} less.
    classLevel(2, "{1}{R}{W}") {
        staticAbility {
            ability = ReduceEquipCost(2)
        }
    }

    // Level 3: each attacking creature you control may lure up to one creature into blocking it.
    classLevel(3, "{3}{R}{W}") {
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
            val blocker = target(TargetFilter.Creature, optional = true)
            effect = Effects.ForceBlock(blocker, attacker = EffectTarget.TriggeringEntity)
            description = "Whenever a creature you control attacks, up to one target creature blocks " +
                "it this combat if able."
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d54a8329-b940-42c9-8ace-1d74407d14cb.jpg?1783926448"
        ruling("2021-07-23", "The discount to equip abilities applies only to generic mana requirements. If you control multiple Fighter Classes that are level 2 or higher, equip abilities cost {2} less for each one. This can't make an ability cost less than {0}.")
        ruling("2021-07-23", "For the last triggered ability, if the target creature is tapped or is affected by a spell or ability that says it can't block, then it doesn't block. If there's a cost associated with having that creature block, its controller isn't forced to pay that cost, so it doesn't have to block in that case either.")
        ruling("2021-07-23", "If the last triggered ability triggers more than once in the same combat, a single creature may be required to block more than one creature. As long as it doesn't have an ability that would allow that, its controller choose which attacking creature it blocks.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
