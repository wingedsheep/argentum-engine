package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oketra's Avenger — Hour of Devastation #17 (canonical printing)
 * {1}{W} · Creature — Human Warrior · 3/1
 *
 * You may exert this creature as it attacks. When you do, prevent all combat damage that would be
 * dealt to it this turn.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" shield is its linked self-exert trigger, a
 * combat-only [Effects.PreventDamage] on itself lasting until end of turn.
 */
val OketrasAvenger = card("Oketra's Avenger") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    oracleText = "You may exert this creature as it attacks. When you do, prevent all combat damage " +
        "that would be dealt to it this turn. (An exerted creature won't untap during your next untap step.)"
    power = 3
    toughness = 1

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Effects.PreventDamage(target = EffectTarget.Self, combatOnly = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Anthony Palumbo"
        flavorText = "\"Oketra's arrow is my spear, and her love my shield.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06a644b4-a2f7-4219-935b-237936b07948.jpg?1783936060"
        ruling("2017-04-18", "You can’t exert a creature unless an effect allows you to do so. Similar effects that “tap and freeze” a creature (such as that of Decision Paralysis) don’t exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert’s effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player’s creature until end of turn and exert it, it will untap during that player’s untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can’t do so later in combat, and creatures put onto the battlefield attacking can’t be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
