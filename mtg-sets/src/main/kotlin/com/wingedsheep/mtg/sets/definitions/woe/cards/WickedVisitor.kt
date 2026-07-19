package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wicked Visitor
 * {1}{B}
 * Creature — Nightmare
 * 2/2
 *
 * Whenever an enchantment you control is put into a graveyard from the battlefield, each opponent
 * loses 1 life.
 *
 * Like Warehouse Tabby, the trigger fires on *any* enchantment you control hitting the graveyard
 * from the battlefield — destroyed, sacrificed, or self-sacrificing (Hopeless Nightmare) and the
 * Role tokens that fall off when replaced — so it uses the generic `leavesBattlefield` factory
 * with an `ANY` binding rather than the SELF-bound named constants.
 */
val WickedVisitor = card("Wicked Visitor") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare"
    power = 2
    toughness = 2
    oracleText = "Whenever an enchantment you control is put into a graveyard from the battlefield, " +
        "each opponent loses 1 life."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Nicholas Gregory"
        flavorText = "Amalgamations of many childhood nightmares stalked the realm of dreams, feeding on the " +
            "collective terror of all those trapped in the Wicked Slumber."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e26ec4b8-0012-48c4-9ccb-0f062df0c250.jpg?1783915099"
    }
}
