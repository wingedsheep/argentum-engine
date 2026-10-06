package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Cone of Flame
 * {3}{R}{R}
 * Sorcery
 * Cone of Flame deals 1 damage to any target, 2 damage to another target, and 3 damage to a
 * third target.
 *
 * The second and third slots are [TargetOther] over [Targets.Any], so each must differ from every
 * target chosen for an earlier slot — all three are distinct, as the ruling requires.
 */
val ConeOfFlame = card("Cone of Flame") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Cone of Flame deals 1 damage to any target, 2 damage to another target, and 3 damage to a third target."

    spell {
        val first = target(Targets.Any)
        val second = target(TargetOther(Targets.Any))
        val third = target(TargetOther(Targets.Any))
        effect = Effects.DealDamage(1, first) then
            Effects.DealDamage(2, second) then
            Effects.DealDamage(3, third)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Ron Spencer"
        flavorText = "\"Mine is not the warmth of compassion.\"\n—Ertai, wizard adept"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5713f17a-9a57-41f8-b492-ced876e1a37f.jpg?1783946729"
        ruling("2014-07-18", "Each of the three targets must be different. If there aren't three different legal targets available, you can't cast the spell.")
        ruling("2014-07-18", "If one or two of Cone of Flame's targets are illegal when it resolves, you can't change how much damage will be dealt to the remaining legal targets.")
    }
}
