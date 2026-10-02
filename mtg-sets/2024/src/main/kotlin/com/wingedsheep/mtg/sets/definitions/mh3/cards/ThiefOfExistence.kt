package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thief of Existence
 * {1}{C}{G} Creature — Eldrazi 3/4
 *
 * The cast trigger resolves while Thief is still a spell, so "Thief of Existence gains …" lands on
 * the creature *spell*; CR 400.7a carries that grant onto the permanent it becomes. If Thief is
 * countered in response the trigger still resolves (ruling) and the grant dies with the stack
 * object. "If you do" reads whether the chosen target really went to exile — no target chosen, or
 * an illegal one (the whole ability then doesn't resolve), grants nothing.
 */
val ThiefOfExistence = card("Thief of Existence") {
    manaCost = "{1}{C}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi"
    power = 3
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "When you cast this spell, exile up to one target noncreature, nonland permanent an opponent " +
        "controls with mana value 4 or less. If you do, Thief of Existence gains \"When this creature " +
        "leaves the battlefield, target opponent draws a card.\""

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val stolen = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.notCreature().opponentControls().manaValueAtMost(4)
            ),
            optional = true
        )
        effect = Effects.Exile(stolen) then Effects.If(
            Conditions.TargetMatchesFilter(GameObjectFilter.Any.currentlyIn(Zone.EXILE), stolen),
            then = Effects.GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.leaves()
                    val opponent = target(Targets.Opponent)
                    effect = Effects.DrawCards(1, opponent)
                    description = "When this creature leaves the battlefield, target opponent draws a card."
                },
                target = EffectTarget.Self,
                duration = Duration.Permanent
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Raph Lomotan"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/328b02ca-d8eb-401d-9c41-93f8eb909312.jpg?1783911254"
        ruling("2024-06-07", "Thief of Existence's triggered ability will resolve before Thief of Existence does. If Thief of Existence is countered or otherwise leaves the stack in response to that triggered ability, the triggered ability will still resolve as normal.")
        ruling("2024-06-07", "If the target of Thief of Existence's triggered ability is illegal as the ability tries to resolve, it won't resolve and none of its effects will happen. Thief of Existence won't gain another ability.")
    }
}
