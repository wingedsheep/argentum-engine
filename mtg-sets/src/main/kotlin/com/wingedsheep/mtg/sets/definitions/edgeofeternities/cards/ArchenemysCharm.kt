package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Archenemy's Charm
 * {B}{B}{B}
 * Instant
 * Choose one —
 * • Exile target creature or planeswalker.
 * • Return one or two target creature and/or planeswalker cards from your graveyard to your hand.
 * • Put two +1/+1 counters on target creature you control. It gains lifelink until end of turn.
 */
val ArchenemysCharm = card("Archenemy's Charm") {
    manaCost = "{B}{B}{B}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Exile target creature or planeswalker.\n• Return one or two target creature and/or planeswalker cards from your graveyard to your hand.\n• Put two +1/+1 counters on target creature you control. It gains lifelink until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Exile target creature or planeswalker") {
                val target = target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
                effect = Effects.Exile(target)
            }
            mode("Return one or two target creature and/or planeswalker cards from your graveyard to your hand") {
                val targets = target("target creature and/or planeswalker cards", TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.GRAVEYARD), count = 2, minCount = 1))
                effect = Effects.ReturnToHand(targets)
            }
            mode("Put two +1/+1 counters on target creature you control. It gains lifelink until end of turn") {
                val target = target("target creature you control", Targets.CreatureYouControl)
                effect = Effects.AddCounters("+1+1", 2, target) then Effects.GrantKeyword(com.wingedsheep.sdk.core.Keyword.LIFELINK, target)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Brigitte Roka & Clifton Stommel"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcde5f27-e2f0-4d2a-afa3-f300896ec4b1.jpg?1752946908"
    }
}
