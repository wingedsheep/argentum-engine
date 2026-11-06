package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gust Walker — Amonkhet #17 (canonical printing)
 * {1}{W} · Creature — Human Wizard · 2/2
 *
 * You may exert this creature as it attacks. When you do, it gets +1/+1 and gains flying until
 * end of turn.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger, which
 * resolves before blockers are declared (so the flying matters for blocks).
 */
val GustWalker = card("Gust Walker") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    oracleText = "You may exert this creature as it attacks. When you do, it gets +1/+1 and gains flying " +
        "until end of turn. (An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 2

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Jason Rainville"
        flavorText = "\"I'm never unarmed.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49307ab6-392e-4b61-ad60-e6ea53da39f7.jpg?1783936537"
        ruling("2017-04-18", "You can’t exert a creature unless an effect allows you to do so. Similar effects that “tap and freeze” a creature (such as that of Decision Paralysis) don’t exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert’s effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player’s creature until end of turn and exert it, it will untap during that player’s untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can’t do so later in combat, and creatures put onto the battlefield attacking can’t be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
