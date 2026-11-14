package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enthralling Victor
 * {3}{R}
 * Creature — Human Warrior
 * 3/2
 *
 * When this creature enters, gain control of target creature an opponent controls with power 2
 * or less until end of turn. Untap that creature. It gains haste until end of turn.
 */
val EnthrallingVictor = card("Enthralling Victor") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "When this creature enters, gain control of target creature an opponent controls with power 2 or less until end of turn. Untap that creature. It gains haste until end of turn. (It can attack and {T} this turn.)"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.opponentControls().powerAtMost(2))
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Winona Nelson"
        flavorText = "If you stare at the sun for too long, you'll lose your ability to see clearly."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3607d63b-82ab-44f0-aef5-d9063e17c76f.jpg?1783938331"
        ruling(
            "2018-03-16",
            "Once Enthralling Victor's ability resolves and you gain control of a creature, raising its " +
                "power above 2 won't cause you to lose control of it. Similarly, it doesn't matter if " +
                "Enthralling Victor leaves the battlefield or you lose control of Enthralling Victor. You'll " +
                "keep control of the creature that was the target of the ability until the end of the turn.",
        )
        ruling(
            "2018-03-16",
            "Enthralling Victor's ability can target a creature that's already untapped. You'll still gain " +
                "control of it and it will gain haste.",
        )
    }
}
