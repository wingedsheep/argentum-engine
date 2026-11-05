package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rhonas's Stalwart — Hour of Devastation #133 (canonical printing)
 * {1}{G} · Creature — Human Warrior · 2/2
 *
 * You may exert this creature as it attacks. When you do, it gets +1/+1 until end of turn and
 * can't be blocked by creatures with power 2 or less this turn.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger, which
 * pumps the creature and grants it [CantBeBlockedBy] over projected power until end of turn.
 */
val RhonassStalwart = card("Rhonas's Stalwart") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "You may exert this creature as it attacks. When you do, it gets +1/+1 until end of turn " +
        "and can't be blocked by creatures with power 2 or less this turn. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 2

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then
            Effects.GrantStaticAbility(
                CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2)),
                EffectTarget.Self
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Bram Sels"
        flavorText = "\"Give me some room.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99d4f791-9084-4644-a1f5-85adbf6dc790.jpg?1783936013"
        ruling("2017-07-14", "Once a creature with power 3 or greater has blocked an exerted Rhonas’s Stalwart, changing the power of the blocking creature won’t cause Rhonas’s Stalwart to become unblocked.")
        ruling("2017-04-18", "You can’t exert a creature unless an effect allows you to do so. Similar effects that “tap and freeze” a creature (such as that of Decision Paralysis) don’t exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert’s effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player’s creature until end of turn and exert it, it will untap during that player’s untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can’t do so later in combat, and creatures put onto the battlefield attacking can’t be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
