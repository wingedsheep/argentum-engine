package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Shield Mare — Core Set 2019 #37
 * {1}{W}{W} · Creature — Horse · 2/3
 *
 * This creature can't be blocked by red creatures.
 * When this creature enters or becomes the target of a spell or ability an opponent controls,
 * you gain 3 life.
 *
 * The evasion is Elder Spawn's `CantBeBlockedBy` red-creature filter; the trigger is one ability
 * with two trigger events, `Triggers.or(enters, becomesTarget(byOpponent))` (Goldspan Dragon's shape).
 */
val ShieldMare = card("Shield Mare") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Horse"
    power = 2
    toughness = 3
    oracleText = "This creature can't be blocked by red creatures.\n" +
        "When this creature enters or becomes the target of a spell or ability an opponent controls, " +
        "you gain 3 life."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.RED))
    }

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.self.enters(),
            Triggers.self.becomesTarget(byOpponent = true),
        )
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Lucas Graciano"
        flavorText = "When it passes, order defeats chaos."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d21e0516-4430-4292-850b-f5c524d7f8f8.jpg?1783934596"
    }
}
