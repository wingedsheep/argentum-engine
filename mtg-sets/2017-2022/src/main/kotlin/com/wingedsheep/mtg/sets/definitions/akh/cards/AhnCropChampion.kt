package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ahn-Crop Champion — Amonkhet #194 (canonical printing)
 * {2}{G}{W} · Creature — Human Warrior · 4/4
 *
 * You may exert this creature as it attacks. When you do, untap all other creatures you control.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger.
 * Untapping attackers doesn't remove them from combat.
 */
val AhnCropChampion = card("Ahn-Crop Champion") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Warrior"
    oracleText = "You may exert this creature as it attacks. When you do, untap all other creatures you control. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 4
    toughness = 4

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Patterns.Group.untapGroup(GroupFilter.AllCreaturesYouControl.other())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Craig J Spearing"
        flavorText = "\"The Second Sun nears its destination! Let it light the way to ours!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2ddda94a-4b2d-47b0-8d2c-3ad70a0d5584.jpg?1783936464"
        ruling("2017-04-18", "All attackers are chosen at once. You can’t attack with Ahn-Crop Champion, untap a tapped creature, and then attack with that creature.")
        ruling("2017-04-18", "Untapping an attacking creature doesn’t remove it from combat.")
        ruling("2017-04-18", "If you attack with two Ahn-Crop Champions and exert both, each will untap the other.")
        ruling("2017-04-18", "You can’t exert a creature unless an effect allows you to do so. Similar effects that “tap and freeze” a creature (such as that of Decision Paralysis) don’t exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert’s effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player’s creature until end of turn and exert it, it will untap during that player’s untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can’t do so later in combat, and creatures put onto the battlefield attacking can’t be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
