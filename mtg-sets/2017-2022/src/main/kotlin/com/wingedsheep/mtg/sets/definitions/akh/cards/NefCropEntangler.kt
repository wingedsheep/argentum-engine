package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nef-Crop Entangler — Amonkhet #144 (canonical printing)
 * {1}{R} · Creature — Human Warrior · 2/1
 *
 * Trample
 * You may exert this creature as it attacks. When you do, it gets +1/+2 until end of turn.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger, which
 * resolves before blockers are declared.
 */
val NefCropEntangler = card("Nef-Crop Entangler") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's " +
        "attacking.)\nYou may exert this creature as it attacks. When you do, it gets +1/+2 until end of " +
        "turn. (An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 1

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Effects.ModifyStats(1, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Dan Murayama Scott"
        flavorText = "\"If you do not feel the thrill of battle, alive like a flame, you will never triumph.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96569dc0-ea94-4a43-b002-85c5066e5386.jpg?1783936484"
        ruling("2017-04-18", "You can't exert a creature unless an effect allows you to do so. Similar effects that \"tap and freeze\" a creature (such as that of Decision Paralysis) don't exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert's effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player's creature until end of turn and exert it, it will untap during that player's untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
