package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Storm Sculptor
 * {3}{U}
 * Creature — Merfolk Wizard
 * 3/2
 * This creature can't be blocked.
 * When this creature enters, return a creature you control to its owner's hand.
 *
 * The evasion is the source-scoped [CantBeBlocked] static. The bounce doesn't target: the creature
 * is chosen as the trigger resolves (Gather → Select → Move), and it isn't optional — the Sculptor
 * itself is a legal choice, and the only one if you control no other creature.
 */
val StormSculptor = card("Storm Sculptor") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 3
    toughness = 2
    oracleText = "This creature can't be blocked.\nWhen this creature enters, return a creature you control to its owner's hand."

    staticAbility {
        ability = CantBeBlocked()
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, return a creature you control to its owner's hand."
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                )
            )
            val bounced = chooseExactly(
                1,
                from = candidates,
                prompt = "Return a creature you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(bounced)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Sidharth Chaturvedi"
        flavorText = "In his hands, the wind can become a weapon or a means of escape."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9532b735-8390-4379-ae43-2bd00d281dd6.jpg?1783935768"
        ruling("2017-09-29", "Storm Sculptor's last ability doesn't target the creature you'll return to hand. You choose one as the ability resolves. No player may take actions between the time you choose a creature to return and the time you do so.")
        ruling("2017-09-29", "Storm Sculptor's last ability isn't optional. If Storm Sculptor is the only creature you control when the ability resolves, you'll have to return it to its owner's hand.")
    }
}
