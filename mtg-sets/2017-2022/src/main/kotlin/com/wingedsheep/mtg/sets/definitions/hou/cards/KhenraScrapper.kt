package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Khenra Scrapper — Hour of Devastation #100 (canonical printing)
 * {2}{R} · Creature — Jackal Warrior · 2/3
 *
 * Menace
 * You may exert this creature as it attacks. When you do, it gets +2/+0 until end of turn.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" pump is its linked self-exert trigger.
 */
val KhenraScrapper = card("Khenra Scrapper") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Jackal Warrior"
    oracleText = "Menace\n" +
        "You may exert this creature as it attacks. When you do, it gets +2/+0 until end of turn. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 3

    keywords(Keyword.MENACE)

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Jesper Ejsing"
        flavorText = "\"There is no tomorrow, so hold nothing back.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5d4caac-95e8-47bb-90c4-3bf877afdc57.jpg?1783936026"
        ruling("2017-04-18", "You can’t exert a creature unless an effect allows you to do so. Similar effects that “tap and freeze” a creature (such as that of Decision Paralysis) don’t exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert’s effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player’s creature until end of turn and exert it, it will untap during that player’s untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can’t do so later in combat, and creatures put onto the battlefield attacking can’t be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
