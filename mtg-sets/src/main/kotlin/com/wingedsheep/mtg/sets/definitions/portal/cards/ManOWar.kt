package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Man-o'-War
 * {2}{U}
 * Creature - Jellyfish
 * 2/2
 * When Man-o'-War enters, return target creature to its owner's hand.
 */
val ManOWar = card("Man-o'-War") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Jellyfish"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = OnEnterBattlefield()
        target = TargetCreature()
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Una Fricker"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e835b618-83c1-46e2-b8bd-aec56f58ccfc.jpg"
    }
}
