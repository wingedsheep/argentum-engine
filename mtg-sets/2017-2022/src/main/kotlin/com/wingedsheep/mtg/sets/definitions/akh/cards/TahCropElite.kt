package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tah-Crop Elite — Amonkhet #31 (canonical printing)
 * {3}{W} · Creature — Bird Warrior · 2/2
 *
 * Flying
 * You may exert this creature as it attacks. When you do, creatures you control get +1/+1 until
 * end of turn.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger, which
 * pumps every creature you control (itself included) before blockers are declared.
 */
val TahCropElite = card("Tah-Crop Elite") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Warrior"
    oracleText = "Flying\nYou may exert this creature as it attacks. When you do, creatures you control get " +
        "+1/+1 until end of turn. (An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.ModifyStats(
                power = 1,
                toughness = 1,
                target = EffectTarget.IterationEntity,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce8dda75-a196-47bc-98d8-522eb35e7d1a.jpg?1783936531"
        ruling("2017-04-18", "You can’t exert a creature unless an effect allows you to do so. Similar effects that “tap and freeze” a creature (such as that of Decision Paralysis) don’t exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert’s effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player’s creature until end of turn and exert it, it will untap during that player’s untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can’t do so later in combat, and creatures put onto the battlefield attacking can’t be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
