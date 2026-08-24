package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Flotilla
 * {2}{R}
 * Creature — Goblin
 * 2/2
 * Islandwalk
 * At the beginning of each combat, unless you pay {R}, whenever this creature blocks or becomes
 * blocked by a creature this combat, that creature gains first strike until end of turn.
 *
 * The {R} is protection money: paying buys the Flotilla out of the drawback for that combat, so
 * the payment is the *cost* and installing the rider is the "suffer" half. The rider itself is a
 * delayed trigger watching the Flotilla's own combat, scoped to
 * [DelayedTriggerExpiry.EndOfCombat] — "this combat", not this turn, so declining in one combat
 * phase does not follow the Flotilla into a second one.
 *
 * It fires once per partner, so blocking a band or being gang-blocked hands first strike to every
 * creature involved, which is exactly what the printed text says.
 */
val GoblinFlotilla = card("Goblin Flotilla") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls an Island.)\n" +
        "At the beginning of each combat, unless you pay {R}, whenever this creature blocks or " +
        "becomes blocked by a creature this combat, that creature gains first strike until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.ISLANDWALK)

    triggeredAbility {
        trigger = Triggers.EachCombat
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana("{R}"),
            suffer = CreateDelayedTriggerEffect(
                trigger = Triggers.BlocksOrBecomesBlockedBy(GameObjectFilter.Creature),
                watchedTarget = EffectTarget.Self,
                effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.TriggeringEntity),
                expiry = DelayedTriggerExpiry.EndOfCombat,
            ),
            consequenceDescription = "creatures that fight this creature this combat gain first strike",
        )
        description = "At the beginning of each combat, unless you pay {R}, whenever this creature blocks or becomes blocked by a creature this combat, that creature gains first strike until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Tom Wänerstrand"
        flavorText = "Exceptionally poor sailors, Goblins usually arrived at their destination retching and in no condition to fight."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87024efe-4a74-49fe-a43a-480bed0a650a.jpg?1783947895"
    }
}
