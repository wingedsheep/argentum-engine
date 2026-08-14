package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.YouAttackEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seasoned Consultant — Murders at Karlov Manor #33
 * {1}{W} · Creature — Human Detective · 1/3
 *
 * Whenever you attack with three or more creatures, this creature gets +2/+0 until end of turn.
 *
 * `YouAttackEvent(minAttackers = 3)` with an ANY binding, the Armasaur Guide shape — the
 * Consultant does *not* have to be one of the three attackers, so it pumps even while sitting
 * back on defense. Counted once per declare-attackers, not once per attacker.
 *
 * Per the MKM ruling, the count is locked in at declaration: removing an attacker in response
 * doesn't stop the +2/+0, which falls out of the trigger reading the event rather than
 * re-counting the battlefield on resolution.
 */
val SeasonedConsultant = card("Seasoned Consultant") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Detective"
    power = 1
    toughness = 3
    oracleText = "Whenever you attack with three or more creatures, this creature gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = TriggerSpec(YouAttackEvent(minAttackers = 3), TriggerBinding.ANY)
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Whenever you attack with three or more creatures, this creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Andreas Zafiratos"
        flavorText = "\"I appreciate working with associates whose greatest loyalty is to the job at hand.\"\n" +
            "—Skurad of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0ebfe5d-8819-495d-bf72-b9c28c6fd23e.jpg?1783912918"
    }
}
