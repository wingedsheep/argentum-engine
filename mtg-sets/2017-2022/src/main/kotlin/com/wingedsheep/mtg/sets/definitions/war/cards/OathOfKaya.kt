package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackedDefenders
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oath of Kaya
 * {1}{W}{B}
 * Legendary Enchantment
 *
 * When Oath of Kaya enters, it deals 3 damage to any target and you gain 3 life.
 * Whenever an opponent attacks a planeswalker you control with one or more creatures, Oath of
 * Kaya deals 2 damage to that player and you gain 2 life.
 *
 * The second ability is `isAttacked(defenders = EACH_PLANESWALKER_YOU_CONTROL)`: it fires once
 * per attacked planeswalker (the card's ruling), never for attackers aimed at you, and binds the
 * attacking player as "that player".
 */
val OathOfKaya = card("Oath of Kaya") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Enchantment"
    oracleText = "When Oath of Kaya enters, it deals 3 damage to any target and you gain 3 life.\n" +
        "Whenever an opponent attacks a planeswalker you control with one or more creatures, " +
        "Oath of Kaya deals 2 damage to that player and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t) then Effects.GainLife(3)
    }

    triggeredAbility {
        trigger = Triggers.you.isAttacked(defenders = AttackedDefenders.EACH_PLANESWALKER_YOU_CONTROL)
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Wesley Burt"
        flavorText = "\"So everyone gets what they truly deserve, I will keep watch.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/a/faba010d-5b1b-4562-91d6-ec55303c4dcd.jpg?1783933391"

        ruling("2019-05-03", "If the target is an illegal target by the time the first ability of Oath of Kaya tries to resolve, the ability doesn't resolve. You won't gain 3 life.")
        ruling("2019-05-03", "If an opponent attacks more than one planeswalker you control, the last ability of Oath of Kaya triggers once for each of those attacked planeswalkers.")
    }
}
