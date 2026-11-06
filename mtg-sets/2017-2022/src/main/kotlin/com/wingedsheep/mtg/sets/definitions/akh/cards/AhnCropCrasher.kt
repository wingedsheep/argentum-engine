package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ahn-Crop Crasher — Amonkhet #117 (canonical printing)
 * {2}{R} · Creature — Minotaur Warrior · 3/2
 *
 * Haste
 * You may exert this creature as it attacks. When you do, target creature can't block this turn.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger, whose
 * target is chosen as it goes on the stack — before blockers are declared.
 */
val AhnCropCrasher = card("Ahn-Crop Crasher") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "You may exert this creature as it attacks. When you do, target creature can't block this turn. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 3
    toughness = 2

    keywords(Keyword.HASTE)

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Seb McKinnon"
        flavorText = "\"Hesitation breeds dissension. To be worthy of the next life, you must never look back.\"\n—Anep, vizier of Hazoret"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd5db68c-24d5-44ce-9809-e446f26fac5d.jpg?1783936494"
        ruling("2017-04-18", "If a creature has a targeted triggered ability that triggers when you exert it, you can exert it even if there isn't a legal target for that triggered ability.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert's effect preventing it from untapping expires without having done anything.")
    }
}
