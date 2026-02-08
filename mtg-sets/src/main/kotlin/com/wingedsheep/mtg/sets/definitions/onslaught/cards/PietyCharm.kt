package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeywordToGroupEffect
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Piety Charm
 * {W}
 * Instant
 * Choose one —
 * • Destroy target Aura attached to a creature.
 * • Target Soldier creature gets +2/+2 until end of turn.
 * • Creatures you control gain vigilance until end of turn.
 */
val PietyCharm = card("Piety Charm") {
    manaCost = "{W}"
    typeLine = "Instant"

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target Aura attached to a creature") {
                target = TargetPermanent(filter = TargetFilter.Enchantment.withSubtype("Aura"))
                effect = MoveToZoneEffect(
                    target = EffectTarget.ContextTarget(0),
                    destination = Zone.GRAVEYARD,
                    byDestruction = true
                )
            }
            mode("Target Soldier creature gets +2/+2 until end of turn") {
                target = TargetCreature(filter = TargetFilter.Creature.withSubtype("Soldier"))
                effect = Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0))
            }
            mode("Creatures you control gain vigilance until end of turn") {
                effect = GrantKeywordToGroupEffect(
                    keyword = Keyword.VIGILANCE,
                    filter = GroupFilter.AllCreaturesYouControl
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "David Martin"
    }
}
