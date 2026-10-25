package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dancing Sword
 * {1}{W} · Artifact — Equipment
 * Equipped creature gets +2/+1.
 * When equipped creature dies, you may have this Equipment become a 2/1 Construct artifact
 * creature with flying and ward {1}. If you do, it isn't an Equipment.
 * Equip {1}
 *
 * The animation is permanent and drops the Equipment subtype (`removeSubtypes`), so the Sword can
 * no longer equip — and, as a creature, couldn't anyway (CR 301.5c).
 */
val DancingSword = card("Dancing Sword") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+1.\n" +
        "When equipped creature dies, you may have this Equipment become a 2/1 Construct artifact creature " +
        "with flying and ward {1}. If you do, it isn't an Equipment. " +
        "(Whenever it becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "Equip {1}"

    staticAbility {
        ability = ModifyStats(+2, +1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.May(
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 2,
                toughness = 1,
                keywords = setOf(Keyword.FLYING),
                creatureTypes = setOf("Construct"),
                removeSubtypes = setOf("Equipment"),
                duration = Duration.Permanent,
            ) then Effects.GrantStaticAbility(
                GrantWard(WardCost.Mana("{1}"), Filters.Self),
                EffectTarget.Self,
                Duration.Permanent,
            ),
            descriptionOverride = "You may have this Equipment become a 2/1 Construct artifact creature " +
                "with flying and ward {1}. If you do, it isn't an Equipment.",
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c3d7824-1297-408c-a11f-0622cab9c4c3.jpg?1783926537"
    }
}
