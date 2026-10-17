package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quiet Contemplation
 * {2}{U}
 * Enchantment
 * Whenever you cast a noncreature spell, you may pay {1}.
 * If you do, tap target creature an opponent controls. It doesn't untap
 * during its controller's next untap step.
 */
val QuietContemplation = card("Quiet Contemplation") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a noncreature spell, you may pay {1}. If you do, tap target creature an opponent controls. It doesn't untap during its controller's next untap step."

    triggeredAbility {
        val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}"),
            then = Effects.Tap(creatureOpponentControls) then
                Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, creatureOpponentControls, Duration.UntilAfterAffectedControllersNextUntap())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dc7c9d5-904e-4ab6-9773-5c4f1c6d34c4.jpg?1562787233"
    }
}
