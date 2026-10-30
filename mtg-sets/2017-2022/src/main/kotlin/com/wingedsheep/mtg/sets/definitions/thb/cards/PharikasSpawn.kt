package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pharika's Spawn
 * {3}{B}
 * Creature — Gorgon
 * 3/4
 * Escape—{5}{B}, Exile three other cards from your graveyard.
 * This creature escapes with two +1/+1 counters on it. When it enters this way, each opponent
 * sacrifices a non-Gorgon creature of their choice.
 *
 * "Escapes with two +1/+1 counters" is Ox of Agonas's [EntersWithCounters] gated on
 * [Conditions.Escaped]. "When it enters this way" is an enters trigger gated by `triggerRestriction =
 * Conditions.Escaped` — checked only as the trigger fires, so it doesn't trigger at all when the
 * Spawn is hard-cast or put onto the battlefield some other way, and (unlike an intervening "if")
 * still resolves if the Spawn has left the battlefield by then.
 * The edict over [Player.EachOpponent] has each opponent choose in turn order and sacrifices every
 * chosen creature at once (the ruling).
 */
val PharikasSpawn = card("Pharika's Spawn") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon"
    power = 3
    toughness = 4
    oracleText = "Escape—{5}{B}, Exile three other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with two +1/+1 counters on it. When it enters this way, each opponent " +
        "sacrifices a non-Gorgon creature of their choice."

    keywordAbility(KeywordAbility.escape("{5}{B}", Costs.additional.ExileOtherCards(3)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    triggeredAbility {
        trigger = Triggers.self.enters()
        triggerRestriction = Conditions.Escaped
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.notSubtype(Subtype("Gorgon")),
            1,
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
        description = "When it enters this way, each opponent sacrifices a non-Gorgon creature of their choice."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2c8244b-7d86-4dff-9419-3945beb2a7b7.jpg?1783931561"
        ruling(
            "2020-01-24",
            "As the triggered ability resolves, first the next opponent in turn order (or, if it's an opponent's " +
                "turn, the opponent whose turn it is) chooses a creature they control, then each other opponent " +
                "in turn order does the same, knowing the choices made before them. Then all the chosen " +
                "creatures are sacrificed at the same time."
        )
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
