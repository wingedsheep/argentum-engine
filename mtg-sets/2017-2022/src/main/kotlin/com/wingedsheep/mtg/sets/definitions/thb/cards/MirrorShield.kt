package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirror Shield
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +0/+2 and has hexproof and "Whenever a creature with deathtouch blocks or
 * becomes blocked by this creature, destroy that creature."
 * Equip {2}
 *
 * The quoted trigger is granted to the equipped creature (Dead-Iron Sledge's idiom), with the
 * per-partner `blocksOrBecomesBlocked(by)` form: the combat partner is the triggering entity, and
 * the deathtouch check is made against projected state as blocks are declared, so a creature that
 * gains deathtouch only afterwards doesn't trigger it (the third ruling). The destroy resolves in the
 * declare blockers step, before combat damage (the first ruling).
 */
val MirrorShield = card("Mirror Shield") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +0/+2 and has hexproof and \"Whenever a creature with deathtouch " +
        "blocks or becomes blocked by this creature, destroy that creature.\"\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(0, 2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.blocksOrBecomesBlocked(
                    by = GameObjectFilter.Creature.withKeyword(Keyword.DEATHTOUCH)
                ),
                effect = Effects.Destroy(EffectTarget.TriggeringEntity),
                descriptionOverride = "Whenever a creature with deathtouch blocks or becomes blocked by this " +
                    "creature, destroy that creature."
            ),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7624e84-93ce-4983-8624-ebc934cab67f.jpg?1783931516"
        ruling(
            "2020-01-24",
            "The ability Mirror Shield grants triggers and resolves before combat damage is dealt. That creature " +
                "won't deal combat damage to the equipped creature or be dealt damage by it."
        )
        ruling(
            "2020-01-24",
            "If a creature with deathtouch blocks the equipped creature and is then destroyed, the equipped " +
                "creature remains blocked. Unless the equipped creature has trample, it won't deal combat damage " +
                "to the player or planeswalker it's attacking."
        )
        ruling(
            "2020-01-24",
            "Once a creature without deathtouch has blocked or become blocked by the equipped creature, causing " +
                "the other creature to gain deathtouch won't cause the ability granted by Mirror Shield to trigger."
        )
    }
}
