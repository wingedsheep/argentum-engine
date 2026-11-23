package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Incursion Specialist
 * {1}{U}
 * Creature — Human Wizard
 * Whenever you cast your second spell each turn, this creature gets +2/+0 until end of turn and
 * can't be blocked this turn.
 * 1/3
 */
val IncursionSpecialist = card("Incursion Specialist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Whenever you cast your second spell each turn, this creature gets +2/+0 until end of turn and can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.you.castsNth(2)
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "Whenever you cast your second spell each turn, this creature gets +2/+0 until end of turn and can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Svetlin Velinov"
        flavorText = "\"Work for the Dimir again? That depends. Tell me more about this maze.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/290e56e0-e699-413a-9d6a-e740bf460b35.jpg?1783940137"
    }
}
