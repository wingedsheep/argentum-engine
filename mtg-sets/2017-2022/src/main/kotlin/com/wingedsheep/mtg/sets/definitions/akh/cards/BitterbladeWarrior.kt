package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bitterblade Warrior — Amonkhet #157 (canonical printing)
 * {1}{G} · Creature — Jackal Warrior · 2/2
 *
 * You may exert this creature as it attacks. When you do, it gets +1/+0 and gains deathtouch
 * until end of turn.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger.
 */
val BitterbladeWarrior = card("Bitterblade Warrior") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Jackal Warrior"
    oracleText = "You may exert this creature as it attacks. When you do, it gets +1/+0 and gains " +
        "deathtouch until end of turn. (An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 2

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Slawomir Maniak"
        flavorText = "\"The vizier of poisons teaches an unconventional kind of strength.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd54edbb-c35d-4f7f-b5ce-d39b0f766ab8.jpg?1783936479"
        ruling("2017-04-18", "You can’t exert a creature unless an effect allows you to do so. Similar effects that “tap and freeze” a creature (such as that of Decision Paralysis) don’t exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert’s effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player’s creature until end of turn and exert it, it will untap during that player’s untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can’t do so later in combat, and creatures put onto the battlefield attacking can’t be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
