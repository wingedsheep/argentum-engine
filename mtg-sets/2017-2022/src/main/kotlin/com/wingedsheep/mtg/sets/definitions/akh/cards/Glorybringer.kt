package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Glorybringer — Amonkhet #134 (canonical printing)
 * {3}{R}{R} · Creature — Dragon · 4/4
 *
 * Flying, haste
 * You may exert this creature as it attacks. When you do, it deals 4 damage to target non-Dragon
 * creature an opponent controls.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger, whose
 * target is chosen as it goes on the stack — before blockers are declared. Glorybringer is the
 * damage source (the trigger's own source).
 */
val Glorybringer = card("Glorybringer") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying, haste\n" +
        "You may exert this creature as it attacks. When you do, it deals 4 damage to target non-Dragon " +
        "creature an opponent controls. (An exerted creature won't untap during your next untap step.)"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        val creature = target(TargetFilter.Creature.opponentControls().notSubtype(Subtype.DRAGON))
        effect = Effects.DealDamage(4, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Sam Burley"
        flavorText = "What the initiates face in the final trial is completely at Hazoret's discretion."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3277ad99-5682-4baa-b106-de15721876a6.jpg?1783936489"
        ruling("2017-04-18", "If a creature has a targeted triggered ability that triggers when you exert it, you can exert it even if there isn't a legal target for that triggered ability.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert's effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player's creature until end of turn and exert it, it will untap during that player's untap step.")
    }
}
