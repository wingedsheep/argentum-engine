package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Chief Warg — The Hobbit #150
 * {2}{B}{G} · Legendary Creature — Wolf · Uncommon
 * 3/3
 *
 * Menace
 * Ferocious — Whenever you attack while you control a creature with power 4 or greater,
 * you draw a card and lose 1 life.
 *
 * Modeling notes:
 *  - "Whenever you attack" is the once-per-combat group trigger ([Triggers.YouAttack]) — it fires
 *    once no matter how many creatures attack, and it does not require The Chief Warg itself to be
 *    among the attackers.
 *  - The "while you control a creature with power 4 or greater" clause is an intervening-if style
 *    condition checked when the trigger would fire (and again on resolution), which is what
 *    `triggerCondition` models. The Chief Warg is 3/3, so it never satisfies this on its own.
 */
val TheChiefWarg = card("The Chief Warg") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Wolf"
    power = 3
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Ferocious — Whenever you attack while you control a creature with power 4 or greater, " +
        "you draw a card and lose 1 life."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.YouAttack
        triggerCondition = Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4))
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.LoseLife(1, EffectTarget.Controller)
        )
        description = "Ferocious — Whenever you attack while you control a creature with power 4 or " +
            "greater, you draw a card and lose 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Tomas Duchek"
        flavorText = "Even magic rings were not much use against wolves—especially against the evil " +
            "packs that lived near Goblin-infested mountains."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c397a298-bf7f-49d7-a26a-206ccf9e8120.jpg?1784377030"
    }
}
