package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grasping Giant
 * {5}{W}
 * Creature — Giant
 * 5/7
 * Vigilance
 * Whenever this creature becomes blocked by a creature, exile that creature until this creature
 * leaves the battlefield.
 *
 * The filtered `becomesBlocked(by = Creature)` form fires once per blocker with that blocker as the
 * triggering entity (the first ruling). "Until this creature leaves the battlefield" is
 * [Effects.MoveUntilSourceLeaves]: a one-shot duration rather than a leaves trigger, and it exiles
 * nothing if the Giant has already left when the trigger resolves (the third ruling). Removing a
 * blocker doesn't unblock the Giant — that's the engine's ordinary combat rule.
 */
val GraspingGiant = card("Grasping Giant") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant"
    power = 5
    toughness = 7
    oracleText = "Vigilance\n" +
        "Whenever this creature becomes blocked by a creature, exile that creature until this creature leaves the battlefield."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.MoveUntilSourceLeaves(EffectTarget.TriggeringEntity, Zone.EXILE)
        description = "Whenever this creature becomes blocked by a creature, exile that creature until " +
            "this creature leaves the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "288"
        artist = "Jinho Bae"
        flavorText = "Mirinthes thought death was seconds away when the giant's fist closed around him. " +
            "Two days later, he was praying for death's release."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a238aa9c-661e-449a-8f05-70b7954e544f.jpg?1783931492"
        ruling("2020-01-24", "If more than one creature blocks Grasping Giant, its ability triggers once for each of those creatures.")
        ruling("2020-01-24", "Grasping Giant doesn't become unblocked when the creatures blocking it are exiled. Unless it gains trample, it won't deal combat damage.")
        ruling("2020-01-24", "If Grasping Giant leaves the battlefield before its triggered ability resolves, the blocking creature won't be exiled.")
        ruling("2020-01-24", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
