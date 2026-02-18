package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
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
    oracleText = "Choose one \u2014\n\u2022 Destroy target Aura attached to a creature.\n\u2022 Target Soldier creature gets +2/+2 until end of turn.\n\u2022 Creatures you control gain vigilance until end of turn."

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
                effect = ForEachInGroupEffect(
                    filter = GroupFilter.AllCreaturesYouControl,
                    effect = GrantKeywordUntilEndOfTurnEffect(Keyword.VIGILANCE, EffectTarget.Self)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/large/front/1/b/1bc2da43-c0e1-4fbf-b309-a75e105c29c1.jpg?1562901548"
    }
}
