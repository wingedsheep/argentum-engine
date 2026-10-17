package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Luxior, Giada's Gift — Streets of New Capenna #240
 * {1} · Legendary Artifact — Equipment
 *
 * Equipped creature gets +1/+1 for each counter on it.
 * Equipped permanent isn't a planeswalker and is a creature in addition to its other types.
 * (Loyalty abilities can still be activated.)
 * Equip planeswalker {1}
 * Equip {3}
 *
 * - The bonus reads every counter of every kind on the equipped permanent (loyalty counters
 *   included), through [EffectTarget.AffectedEntity] — the permanent the layer-7c effect is
 *   being applied to.
 * - The type change is two layer-4 statics on the attached permanent: remove PLANESWALKER, add
 *   CREATURE. Loyalty SBAs and loyalty-counter damage key off the projected planeswalker type,
 *   so the equipped permanent takes marked damage like any creature; its loyalty abilities stay
 *   activatable because their gate is the ability, not the type. With no printed P/T it is a
 *   0/0 before the counter bonus (ruling).
 * - "Equip planeswalker {1}" is an equip variant targeting a planeswalker you control; the
 *   equip resolution attaches without the creature-host check, and the CR 704.5n check reads
 *   the projected host — which Luxior itself has made a creature — so the attachment holds.
 */
val LuxiorGiadasGift = card("Luxior, Giada's Gift") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each counter on it.\n" +
        "Equipped permanent isn't a planeswalker and is a creature in addition to its other types. " +
        "(Loyalty abilities can still be activated.)\n" +
        "Equip planeswalker {1}\n" +
        "Equip {3}"

    val countersOnIt = DynamicAmounts.countersOn(EffectTarget.AffectedEntity, null)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = countersOnIt,
            toughnessBonus = countersOnIt,
        )
    }

    staticAbility {
        ability = RemoveCardType("PLANESWALKER", filter = GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = GrantCardType("CREATURE", filter = GroupFilter.attachedCreature())
    }

    equipAbility(
        "{1}",
        quality = "planeswalker",
        targetFilter = TargetFilter(GameObjectFilter.Planeswalker.youControl()),
    )

    equipAbility("{3}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "240"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1833662b-ccf3-4c16-9767-666d6407aa65.jpg?1783923061"
        ruling(
            "2022-04-29",
            "\"Equip planeswalker\" is a variant of the equip ability. \"Equip planeswalker [cost]\" means " +
                "\"[Cost]: Attach this permanent to target planeswalker you control as though that " +
                "planeswalker were a creature. Activate only as a sorcery.\""
        )
        ruling(
            "2022-04-29",
            "Unless another effect is making that planeswalker a creature with a power and toughness, " +
                "the equipped planeswalker has base power and toughness 0/0."
        )
    }
}
